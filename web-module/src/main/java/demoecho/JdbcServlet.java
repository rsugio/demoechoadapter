package demoecho;

import com.sap.engine.services.configuration.appconfiguration.ApplicationPropertiesAccess;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class JdbcServlet extends HttpServlet {
    ApplicationPropertiesAccess applicationConfiguration = null;
    Properties config = null;
    DataSource dataSource = null;
    Connection conn = null;
    Exception initException = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            InitialContext ctx = new InitialContext();
            applicationConfiguration = (ApplicationPropertiesAccess) ctx.lookup("ApplicationConfiguration");
            this.config = applicationConfiguration.getApplicationProperties();
            String dataSourceName = this.config.getProperty("jdbc.dataSourceName");
            if (dataSourceName != null && !dataSourceName.isEmpty()) {
                dataSource = (DataSource) ctx.lookup(dataSourceName);
                conn = dataSource.getConnection();
                return;
            }

            String jdbcSelector = this.config.getProperty("jdbc.selector");
            if (jdbcSelector == null || jdbcSelector.isEmpty()) {
                return;
            }
            String driverClassName = this.config.getProperty(jdbcSelector + ".driverClassName");
            String url = this.config.getProperty(jdbcSelector + ".url");
            String username = this.config.getProperty(jdbcSelector + ".username");
            String password = this.config.getProperty(jdbcSelector + ".password");
            if (driverClassName != null && url != null) {
                Class.forName(driverClassName);
                conn = DriverManager.getConnection(url, username, password);
            }
        } catch (NamingException | SQLException | ClassNotFoundException e) {
            initException = e;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (initException != null) {
            resp.sendError(500);
            PrintWriter pw = new PrintWriter(resp.getOutputStream());
            resp.setContentType("text/plain; charset=utf-8");
            pw.printf("Cannot work. Initialization error: %s", initException.getMessage());
            pw.close();
            return;
        }
        PrintWriter pw = new PrintWriter(resp.getOutputStream());
        resp.setContentType("text/plain; charset=utf-8");
        pw.println(JdbcServlet.class.getName());
        pw.println(conn);
        try {
            mysql(pw);
        } catch (Exception e) {
            e.printStackTrace(pw);
        }
        pw.flush();
        pw.close();
    }

    void mysql(PrintWriter pw) throws SQLException {
        String tables = "-- Кастомные таблицы/вьюхи \n" +
                "SELECT table_schema,\n" +
                "    table_name,\n" +
                "    table_type,  -- 'BASE TABLE' или 'VIEW'\n" +
                "    table_rows,\n" +
                "    update_time,\n" +
                "    table_comment\n" +
                "FROM information_schema.tables\n" +
                "WHERE table_schema <> 'information_schema'\n" +
                "ORDER BY table_schema, table_type, table_name;";
        String fields = "-- Структура таблицы (колонки, типы, ключи)\n" +
                "SELECT table_name, ordinal_position, \n" +
                "    column_name,\n" +
                "    data_type,\n" +
                "    column_type,\n" +
                "    is_nullable,\n" +
                "    column_default,\n" +
                "    column_key,  -- PRI = первичный ключ, MUL = внешний/индекс\n" +
                "    extra,\n" +
                "    character_maximum_length,\n" +
                "    numeric_precision,\n" +
                "    numeric_scale,\n" +
                "    column_comment\n" +
                "FROM information_schema.columns\n" +
                "WHERE table_schema <> 'information_schema'\n" +
                "ORDER BY table_schema, table_name, ordinal_position;";
        String[] queries = {"SELECT VERSION()",
                "SHOW DATABASES",
                tables,
                fields,
                "SHOW STATUS",
                "SHOW VARIABLES",};
        for (String s: queries) {
            pw.println(s);
            executeStatement(pw, conn.prepareStatement(s));
            pw.println();
        }
        ResultSet rs = conn.prepareCall(tables).executeQuery();
        while (rs.next()) {
            String tableSchema = rs.getString(1);
            String tableName = rs.getString(2);
            String s = "SELECT * FROM " + tableSchema + "." + tableName + " LIMIT 100";
            pw.println(s);
            try {
                executeStatement(pw, conn.prepareStatement(s));
            } catch (SQLException e) {
                pw.println(e.getMessage());
            }
            pw.println();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    public void destroy() {
        try {
            if (conn != null) {
                conn.close();
            }
            if (dataSource != null && dataSource instanceof AutoCloseable) {
                ((AutoCloseable) dataSource).close();
            }
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    public void executeStatement(PrintWriter pw, PreparedStatement ps) throws SQLException {
        // Проверяем, является ли запрос SELECT
        boolean isResultSet = ps.execute();

        if (!isResultSet) {
            // Для UPDATE/INSERT/DELETE выводим количество затронутых строк
            int updateCount = ps.getUpdateCount();
            pw.println("Query executed successfully. Rows affected: " + updateCount);
            pw.flush();
            return;
        }

        // Обрабатываем ResultSet
        try (ResultSet rs = ps.getResultSet()) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Получаем имена столбцов
            List<String> columnNames = new ArrayList<>();
            List<Integer> columnWidths = new ArrayList<>();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                if (columnName == null || columnName.isEmpty()) {
                    columnName = metaData.getColumnName(i);
                }
                columnNames.add(columnName);
                // Минимальная ширина - длина заголовка
                columnWidths.add(columnName.length());
            }

            // Собираем все строки данных в List для расчета ширины
            List<List<String>> rows = new ArrayList<>();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String value = getStringValue(rs, i, metaData.getColumnType(i));
                    row.add(value);
                    // Обновляем максимальную ширину столбца
                    int currentWidth = columnWidths.get(i - 1);
                    if (value.length() > currentWidth) {
                        columnWidths.set(i - 1, value.length());
                    }
                }
                rows.add(row);
            }

            // Ограничиваем ширину для читаемости (опционально)
            int maxColumnWidth = 100;
            for (int i = 0; i < columnWidths.size(); i++) {
                if (columnWidths.get(i) > maxColumnWidth) {
                    columnWidths.set(i, maxColumnWidth);
                }
            }

            // Строим разделитель
            StringBuilder separator = new StringBuilder("+");
            for (int width : columnWidths) {
                for (int i=0; i<Math.max(0, width + 2); i++)
                    separator.append("-");
                separator.append("+");
            }

            // Выводим заголовок
            pw.println(separator.toString());
            pw.print("|");
            for (int i = 0; i < columnCount; i++) {
                String header = padRight(columnNames.get(i), columnWidths.get(i));
                pw.print(" " + header + " |");
            }
            pw.println();
            pw.println(separator.toString());

            // Выводим данные
            if (rows.isEmpty()) {
                pw.println("| No rows returned |");
            } else {
                for (List<String> row : rows) {
                    pw.print("|");
                    for (int i = 0; i < columnCount; i++) {
                        String value = row.get(i);
                        // Обрезаем длинные значения
                        if (value.length() > columnWidths.get(i)) {
                            value = value.substring(0, columnWidths.get(i) - 3) + "...";
                        }
                        String formatted = padRight(value, columnWidths.get(i));
                        pw.print(" " + formatted + " |");
                    }
                    pw.println();
                }
            }

            // Нижний разделитель
            pw.println(separator.toString());
            pw.println("Total rows: " + rows.size());
            pw.flush();
        }
    }

    /**
     * Преобразует значение из ResultSet в строку с учетом типа данных
     */
    private String getStringValue(ResultSet rs, int columnIndex, int columnType) throws SQLException {
        // Проверяем NULL
        if (rs.getObject(columnIndex) == null) {
            return "NULL";
        }

        switch (columnType) {
            case Types.DATE:
                Date date = rs.getDate(columnIndex);
                return date != null ? date.toString() : "NULL";
            case Types.TIMESTAMP:
                Timestamp timestamp = rs.getTimestamp(columnIndex);
                return timestamp != null ? timestamp.toString() : "NULL";
            case Types.TIME:
                Time time = rs.getTime(columnIndex);
                return time != null ? time.toString() : "NULL";
            case Types.DECIMAL:
            case Types.NUMERIC:
                // Сохраняем точность для чисел
                return rs.getString(columnIndex);
            case Types.BOOLEAN:
            case Types.BIT:
                return String.valueOf(rs.getBoolean(columnIndex));
            case Types.CLOB:
                Clob clob = rs.getClob(columnIndex);
                if (clob == null) return "NULL";
                int length = (int) Math.min(clob.length(), 1000);
                return clob.getSubString(1, length) + (clob.length() > 1000 ? "..." : "");
            case Types.BLOB:
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                Blob blob = rs.getBlob(columnIndex);
                if (blob == null) return "NULL";
                return "[BLOB " + blob.length() + " bytes]";
            case Types.JAVA_OBJECT:
            case Types.OTHER:
                Object obj = rs.getObject(columnIndex);
                return obj != null ? obj.toString() : "NULL";
            default:
                // Для строковых типов и всего остального
                return rs.getString(columnIndex);
        }
    }

    /**
     * Выравнивает строку по левому краю до указанной ширины
     */
    private String padRight(String str, int width) {
        if (str == null) str = "NULL";
        if (str.length() >= width) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
