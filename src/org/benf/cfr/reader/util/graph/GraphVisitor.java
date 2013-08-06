package org.benf.cfr.reader.util.graph;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: lee
 * Date: 13/03/2012
 * Time: 06:08
 * To change this template use File | Settings | File Templates.
 */
public interface GraphVisitor<T> {
    // TODO : Bulk enqueue.
    void enqueue(T next);

    void enqueue(Collection<? extends T> next);

    void process();
}

