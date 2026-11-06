package com.istore;

public enum SortMode {
    COUNT_DESC("Count: high to low"),
    COUNT_ASC("Count: low to high"),
    NAME("Name: A to Z");

    public final String label;

    SortMode(String label) {
        this.label = label;
    }

    public SortMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
