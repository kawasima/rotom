package net.unit8.rotom.model.filter;

import net.unit8.rotom.model.Filter;
import net.unit8.rotom.model.MarkupType;
import net.unit8.rotom.model.Page;

import java.util.Arrays;

public class Render implements Filter {
    @Override
    public String extract(String data, Page page) {
        return Arrays.stream(MarkupType.values())
                .filter(mt -> mt.match(page.getFileName()))
                .findAny()
                .map(mt -> mt.render(data))
                .orElse(data);

    }

    @Override
    public String process(String data, Page page) {
        return data;
    }
}
