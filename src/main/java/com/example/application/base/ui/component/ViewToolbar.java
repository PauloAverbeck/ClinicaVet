package com.example.application.base.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ViewToolbar extends Composite<Header> {

    public ViewToolbar(@Nullable String viewTitle, Component... components) {
        addClassNames(Display.FLEX, FlexDirection.COLUMN, JustifyContent.BETWEEN, AlignItems.STRETCH, Gap.MEDIUM,
                FlexDirection.Breakpoint.Medium.ROW, AlignItems.Breakpoint.Medium.CENTER);

        var drawerToggle = new DrawerToggle();
        drawerToggle.addClassNames(Margin.NONE);

        var title = new H1(viewTitle);
        title.addClassNames(FontSize.XLARGE, Margin.NONE, FontWeight.MEDIUM);

        var toggleAndTitle = new Div(drawerToggle, title);
        toggleAndTitle.addClassNames(Display.FLEX, AlignItems.CENTER);
        getContent().add(toggleAndTitle);

        if (components.length > 0) {
            var actions = new Div(components);
            actions.addClassNames(Display.FLEX, FlexDirection.COLUMN, JustifyContent.BETWEEN, Flex.GROW, Gap.SMALL,
                    FlexDirection.Breakpoint.Medium.ROW);
            getContent().add(actions);
        }

        getContent().getStyle().set("align-self", "flex-start");
        getContent().getStyle().set("margin-right", "auto");
        getContent().getStyle().set("margin-left", "0");
        getContent().getStyle().set("flex-shrink", "0");
        getContent().getStyle().set("width", "100%");
        getContent().getStyle().set("position", "sticky");
        getContent().addClassName("view-toolbar");
    }

    public static Component group(Component... components) {
        var group = new Div(components);
        group.addClassNames(Display.FLEX, FlexDirection.COLUMN, AlignItems.STRETCH, Gap.SMALL,
                FlexDirection.Breakpoint.Medium.ROW, AlignItems.Breakpoint.Medium.CENTER);
        return group;
    }
}
