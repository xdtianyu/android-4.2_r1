
package com.android.quicksearchbox;

import android.content.ComponentName;

import java.util.Collection;

/**
 * Search source set.
 */
public interface Sources {

    /**
     * Gets all sources.
     */
    Collection<Source> getSources();

    /**
     * Gets a source by name.
     *
     * @return A source, or {@code null} if no source with the given name exists.
     */
    Source getSource(String name);

    /**
     * Gets the web search source.
     */
    Source getWebSearchSource();

    /**
     * Creates a new source for a specific component.
     * @param component Name of the component to search
     * @return a new {@code Source} corresponding to {@code component}.
     */
    Source createSourceFor(ComponentName component);

    /**
     * Updates the list of sources.
     */
    void update();

}
