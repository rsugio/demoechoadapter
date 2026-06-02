package demoecho;

import org.apache.wicket.protocol.http.WebApplication;

public class HelloWorldApplication extends WebApplication {

    /**
     * Указывает, какую страницу показывать как домашнюю.
     */
    @Override
    public Class<HomePage> getHomePage() {
        return HomePage.class;
    }

    @Override
    public void init() {
        super.init();
        // Здесь можно добавить конфигурацию приложения, если нужно
    }
}