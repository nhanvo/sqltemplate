package ninja.cero.sqltemplate.core.template;

import java.io.IOException;

public class PlainText implements TemplateEngine {
    @Override
    public String get(String resource, Object[] args) throws IOException {
        return resource;
    }

    @Override
    public String get(String resource, Object object) throws IOException {
        return resource;
    }
}
