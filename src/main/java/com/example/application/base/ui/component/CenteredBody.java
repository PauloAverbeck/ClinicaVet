package com.example.application.base.ui.component;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;

public class CenteredBody extends Composite<Div> {

    public CenteredBody() {
        getContent().getStyle()
                .set("display", "flex")
                .set("flex", "1")
                .set("min-height", "0")
                .set("overflow", "auto")
                .set("justify-content", "center")
                .set("align-items", "center")
                .set("padding", "var(--lumo-space-m)");

        var wrapper = new Div();
        wrapper.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("text-align", "center")
                .set("gap", "var(--lumo-space-m)")
                .set("max-width", "420px")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("margin", "auto");
        getContent().add(wrapper);
    }

    public Div wrapper() {
        return (Div) getContent().getChildren().findFirst().get();
    }
}