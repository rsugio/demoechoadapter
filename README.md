
# Эхоадаптер и скрипт его сборки
mailto:chumpa@yandex.ru, май 2026, Россия

## Зачем это нужно
Во-первых, чтобы наглядный пример минимального исходника был под руками.
Во-вторых, чтобы он собирался современным gradle-9.2 и JDK24.
В-третьих, чтобы было удобно во всём.

## Что на борту?
Gradle-проект состоит из системы сборки (buildSrc), библиотек (libs) и двух модулей: resource-adapter и web-module.

## /libs
Сюда надо положить все саповские библиотеки, необходимые для сборки и локального тестирования проекта:

```text
com.sap.aii.af.cpa.svc.api.jar
com.sap.aii.af.lib.mod.jar
com.sap.aii.af.ms.ifc_api.jar
com.sap.aii.af.svc_api.jar
com.sap.aii.sec.lib_api.jar
com.sap.engine.clientapis_2.0.0.250804134544.jar
com.sap.exception.jar
sap.com~tc~bl~guidgenerator~impl.jar
sap.com~tc~bl~txmanagerimpl~plb~impl.jar
sap.com~tc~je~appcontext_api~API.jar
sap.com~tc~logging~java~impl.jar
sap.com~tc~sec~ssf.jar
```
На сегодня здесь лежат зависимости от 75sp32, так как они не деплоятся и нужны только для сборки -- можете спокойно
использовать свои, добавлять, и не относиться как к чему-то сверхважному.

Также в /libs кладём те библиотеки, которые хотим деплоить отдельной расшаренной DCшкой, сейчас это:
```text
commons-io-2.22.0.jar
commons-lang3-3.20.0.jar
```
Эти библиотеки используются при сборке, в рантайме и деплоятся отдельной компонентой с прописыванием в дескрипторе provider.xml.
Они взяты с https://mvnrepository.com, в /libs выложены насильно для удобного процесса деплоя lib sda.

## :resource-adapter
Двоеточие перед названием в gradle означает имя модуля. Здесь обычный gradle java-library модуль, зависимости указаны
в build.gradle.kts просто:
```kotlin
dependencies {
    implementation(
        fileTree(
            mapOf(
                "dir" to "../libs", "include" to listOf("*.jar")
            )
        )
    )
}
```
Тестирование через JUnit5/JUpiter, но пока оно в зародыше, так как жизненный цикл саповского ресурс-адаптера ещё
не воспроизведён. В целом возможно на базе древней версии TomEE и JCA1.5 написать какую-то песочницу,
смысла в этом мало.

При `gradle :resource-adapter:jar` создаётся `resource-adapter/build/libs/resource-adapter.jar`.

### AdapterTypeMetaData для адаптера -- что мы видим в свойствах канала
Это `resource-adapter/src/main/resources/metadata/Echo.xml`, он создаётся программным кодом
https://github.com/rsugio/komar/blob/04c56633659b334ea0a46b997b4fbf333156b5a2/src/test/java/ParserTests.java#L166
метод adapterTypeMetaData(). Лучше это перенести в buildSrc конечно, пока было сделано для удобной отладки в сравнении
своего адаптера с примерами (SFTP, IDoc_AAE, SampleRA). Я не стал конфигурировать JNDI параметром модуля. 
Возможно, придётся так сделать (в процессе отладки).

Эти метаданные надо загрузить через ESR, в любой SWCV как AdapterMetadata, при этом ВАЖНО!!!
* имя области имён `urn:demo` (конфигурируется в EchoAdapterConstants.adapterNamespace)
* имя объекта `Echo` (конфигурируется в EchoAdapterConstants.adapterType)
Вендор SWCV неважен. То есть, создайте Adapter Metadata `{urn:demo}Echo` и тогда они подхватятся в рантайме.

## resource-adapter/src/main/java/demoecho/EchoAdapterConstants.java
Очень важной частью является центральный конфигурационный файл, константы которого используются при компиляции и сборке
во многих местах. Идеи его создания:
* чтобы имена классов в разных дескрипторах были компилируемыми а не текстовыми строками, и переименование какого-либо
класса или пакета влекло за собой автоперестройку дескрипторов. Это обстоятельство позволяет избежать опечаток и
ручного труда. Частично так умеет NWDS но Eclipse в части сап-плагинов уже не будет развиваться.
* чтобы при сборке RAR, SDA, SCA использовались те же константы, что при компиляции адаптера и в рантайме,
а также в UI сервлета

Сами отдельные константы здесь описывать не буду.

### properties.xml в корне проекта
EchoAdapterConstants.java при запуске `public static void main()` создаёт файл properties.xml с java.util.Properties
в формате xml. Сегодня это:

```xml
<properties>
<comment>Константы для сборки адаптера Echo из файла demoecho.EchoAdapterConstants</comment>
<entry key="raCCIConnectionFactory">demoecho.CCIConnectionFactory</entry>
<entry key="adapterVendor">rsug.io</entry>
<entry key="raEis">Без EIS (локальная обработка)</entry>
<entry key="webContextRoot">rsug.io~demoecho</entry>
<entry key="adapterType">Echo</entry>
<entry key="adapterVersion">1</entry>
<entry key="kolhoz">&#128229; Колхозная, им.тов.Гредлова, система сборки RAR/SDA. Ибо нефиг.</entry>
<entry key="jndi">deployedAdapters/demo.echoadapter.ra/shareable/demo.echoadapter.ra</entry>
<entry key="dcNameRA">demo.echoadapter.ra</entry>
<entry key="swcName">ZRSUGIO</entry>
<entry key="raShortName">demo.echoadapter</entry>
<entry key="dcNameWeb">demo.echoadapter.web</entry>
<entry key="adapterNamespace">urn:demo</entry>
<entry key="raCCIConnection">demoecho.CCIConnection</entry>
<entry key="raDescription">Модель для сборки</entry>
<entry key="dcNameLib">demo.echoadapter.lib</entry>
<entry key="raSPIManagedConnectionFactory">demoecho.SPIManagedConnectionFactory</entry>
<entry key="adapterVendorLocation">Russia, Moscow</entry>
</properties>
```

## :web-module
Здесь попытка сделать минималистический UI для просмотра логов адаптера. 
Удобным файловым местом для них является `/usr/sap/${SAPSYSTEMNAME}/SYS/global/xi_customer_logs`, в том числе с учётом
кластера. Либо можно перенести в другое место конфигурацией в sap.global.app.properties.

Модуль собирается стандартным гредл-плагином `war` и создаёт /web-module/build/libs/web-module.war. 

Адрес вызова (context-root) описан в константах как:
```text
    // http://localhost:50000/rsug.io~demoecho
    public static final String webContextRoot = adapterVendor + "~demoecho";
```
Этот путь попадает в дескриптор application.xml web-sda'шки.

Сам сервлет EchoAdapterServlet очень простой, лишь обрабатывает doGet и показывает текст.
Дескрипторы WEB-INF/web.xml и WEB-INF/web-j2ee-engine.xml пишем исходным кодом (они не генерируются автоматически),
при этом /web-app/@version должно быть `2.5` а /web-j2ee-engine/spec-version `2.4`. Это проверяет саповский линтер
при деплое.

## buildSrc и build.gradle.kts
Это самописная система сборки, которая использует константы из `EchoAdapterConstants`/`properties.xml`, и JAR/WAR
созданные плагинами java-library и war для :resource-adapter и :web-module, а также содержимое `libs/`.

### buildSrc/src/main/java/Dependencies.java
Сюда пишем все стандартные сап-зависимости, которые попадают в разные дескрипторы развёртывания:
* rsug.io~demo.echoadapter.ra.rar/META-INF/connector-j2ee-engine.xml
* rsug.io~demo.echoadapter.ra.sda/META-INF/application-j2ee-engine.xml
* rsug.io~demo.echoadapter.web.sda/META-INF/application-j2ee-engine.xml

Собственные дополнительные зависимости пишем в задаче `sapSdaFromLibs` как:
```kotlin build.gradle.kts
    providedLibs.from(
        project.file("libs/commons-io-2.22.0.jar"),
        project.file("libs/commons-lang3-3.20.0.jar")
    )
 ```
Почему это не в константах? Логика такая:
* buildSrc не скомпилирован как отдельный гредл-плагин а доступен к правке "на месте", для каждого проекта
* стандартные сап-зависимости 99% шаблонны и лишь изредка дополняются, а в константах это будет мешать глазам
* корневой build.gradle.kts вы один фиг будете править, доп.зависимости там органично указаны

### buildSrc/src/main/java/SdaFromLibs.java
Собирает `rsug.io~demo.echoadapter.lib.sda` и сочиняет дескрипторы.

### buildSrc/src/main/java/RarFromJar.java
Собирает `rsug.io~demo.echoadapter.ra.rar` и пишет/копирует дескрипторы.

### buildSrc/src/main/java/SdaFromRar.java
Собирает `rsug.io~demo.echoadapter.ra.sda`, исходный rar не удаляет.

### buildSrc/src/main/java/SdaFromWar.java
Собирает `rsug.io~demo.echoadapter.web.sda`, сочиняет дескрипторы.

### buildSrc/src/main/java/Sca.java
Собирает `ZRSUGIO01_0.sca` из всех SDAшек в папке build, при этом считывает их SAP_MANIFEST.MF
и вносит часть атрибутов в свой основной SAP_MANIFEST.MF.

### Gradle-задачи и их зависимости
* jar, war -- стандартные, без особых настроек
* sapRarFromJar
* sapSdaFromLibs 
* sapSdaFromRar
* sapSdaFromWar
* sap -- зависит от четырёх выше, просто контейнер удобного запуска
* sapSca -- аналогично sap, те же зависимости

## Кароче, Скли-кий! Как собрать самому?
* Создаём проект в idea из https://github.com/rsugio/demoechoadapter.git
* Ставим локально Gradle-9.2, так лучше ))) Или делаем gradle wrapper, позднее добавлю сюда
* ideaj, указываем Project SDK как sap-1.8 (SapMachine-1.8.0_391 у меня сейчас)
* ideaj, указываем в свойствах гредла что GradleVM это liberica-full-24 (BellSoft 24.0.2)
* ideaj, указываем в свойствах гредла - локальный он или враппер

Далее
```bat
gradle clean
gradle sapSca
```
И смотрим в build файл ZRSUGIO01_0.sca. Или ругань в логах. Будет несколько варнингов, большая
часть из кода sampleRa который пока не вычищен от косяков сапа.

## Библиотека io.rsug:komar:0.0.1
Это отдельный репозиторий, минималистический проект и артефакт для работы с саповскими дескрипторами
и конфигами, в основном на базе доступных XSD/DTD, изредка подправленных. Несколько утилитарных
хелперов, JAXB, ничего особенного.

См. https://github.com/rsugio/komar

# Полученные результаты и ограничения
1. Удалось сгенерировать RAR, SDA, SCA которые деплоятся в NWDS 7.5sp32 и sp35
2. Система сборки может копироваться из проекта в проект и дописываться по месту, что-то можно
выложить как готовый гредл-плагин
3. Инфраструктура сборки - современная на 2026г, Gradle 9.2 и JDK24
4. Среда разработки - IdeaJ community позволяет не испытывать боль от NWDS
5. Исходники sampleRA удалось декомпилировать (сходу не нашёл нормального пакета с сырцами)

Ограничения и неудобства:
1. Собираемый ZRSUGIO01_0.sca не содержит BUILDARCHIVES, поэтому не может быть импортирован
в NWDI / NWDS DevelopmentInfrastructure через импорт. Это чисто рантаймовый SCA без публичных частей
и .dcia-файлов с .dcda и .dcdef. DCDEF генерировать не проблема, но это много
утомительного POJO-кода без особой "утилизации" с учётом погибели SAP NetWeaver.
2. Сам адаптер на конец мая 2026 не умеет делать эхо. "Весь пар ушёл в свисток", на сборку и возню
с дескрипторами потрачено время. Это наживное.
3. Нумерация версий проекта подстроена под целочисленную логику /AdapterMetaDataType/@version.
Текущая версия - 1, очень долгое время внутри неё и будет писаться код примера.

Что стоит добавить:
* Job-definition -- полезная штука, пригодится например при сборке логов и для своего мониторинга.
Да и джобы приходится периодически для нетвивера писать, едва ли не чаще адаптеров и сервлетов. 
* сборка EJB для ява-проксей -- с точки зрения покрытия наследия предков. Опять-таки JAXB тот же...
* Apache Wicket 6 или 7 для сервлетов, если оно заработает вообще. Тогда минималистический UI будет
не столь убогим
* с учётом модульности, EchoAdapterConstants надо перенести в src корневого проекта, но здесь проблема
модульности gradle и его зависимостей может всплыть.

# Как вы можете это использовать в своей работе?
Необязательно, что этот репозиторий нужен лишь тем кто пишет адаптеры. Можно выкинуть отсюда
почти весь resource-adapter (оставить лишь константы), а использовать `buildSrc` и `web-module` для
написания и сборки сервлетов.

Для тех, кто пишет адаптеры, это конечно более интересно. Пишите вопросы, предложения, форкайте этот
репозитарий или присылайте коммиты, собирайте своё! Лицензия свободная, в тч для коммерческих целей.

Желаю нам всем удачи и добра.
