package com.jecstar.etm.gui.rest.services.iib;

interface NodeConverter<T> {

    Node read(T content);

    T write(Node node);

    NodeTags getTags();
}
