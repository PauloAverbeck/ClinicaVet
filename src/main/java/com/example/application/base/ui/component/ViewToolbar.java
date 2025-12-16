package com.example.application.base.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ViewToolbar extends Composite<Header> {

    private final H1 title;

    public ViewToolbar(@Nullable String viewTitle, Component... components) {
        Header header = getContent();

        header.addClassNames(
                Display.FLEX,
                FlexDirection.COLUMN,
                JustifyContent.BETWEEN,
                AlignItems.STRETCH,
                Gap.MEDIUM,
                FlexDirection.Breakpoint.Medium.ROW,
                AlignItems.Breakpoint.Medium.CENTER
        );

        this.title = new H1(viewTitle == null ? "" : viewTitle);
        this.title.addClassNames(FontSize.XLARGE, Margin.NONE, FontWeight.MEDIUM);

        var titleWrapper = new Div(this.title);
        titleWrapper.addClassNames(Display.FLEX, AlignItems.CENTER);
        header.add(titleWrapper);

        if (components.length > 0) {
            var actions = new Div(components);
            actions.addClassNames(
                    Display.FLEX,
                    FlexDirection.COLUMN,
                    JustifyContent.BETWEEN,
                    Flex.GROW,
                    Gap.SMALL,
                    FlexDirection.Breakpoint.Medium.ROW,
                    AlignItems.Breakpoint.Medium.CENTER
            );
            header.add(actions);
        }

        header.getStyle().set("align-self", "flex-start");
        header.getStyle().set("margin-right", "auto");
        header.getStyle().set("margin-left", "0");
        header.getStyle().set("flex-shrink", "0");
        header.getStyle().set("width", "100%");
        header.getStyle().set("position", "sticky");
        header.addClassName("view-toolbar");
    }

    public void setTitle(@Nullable String viewTitle) {
        title.setText(viewTitle == null ? "" : viewTitle);
    }

    public String getTitle() {
        return title.getText();
    }

    public static Component group(Component... components) {
        var group = new Div(components);
        group.addClassNames(
                Display.FLEX,
                FlexDirection.COLUMN,
                AlignItems.STRETCH,
                Gap.SMALL,
                FlexDirection.Breakpoint.Medium.ROW,
                AlignItems.Breakpoint.Medium.CENTER
        );
        return group;
    }
}