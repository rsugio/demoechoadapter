package demoecho;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;

public class HomePage extends WebPage {

    public HomePage() {
        // Добавляем компонент Label с идентификатором "message" и текстом
        add(new Label("message", "Привет World! wicket 6.30.0"));

    }
}