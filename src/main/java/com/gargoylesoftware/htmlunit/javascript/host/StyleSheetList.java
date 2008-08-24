/*
 * Copyright (c) 2002-2008 Gargoyle Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gargoylesoftware.htmlunit.javascript.host;

import java.io.StringReader;
import java.util.logging.Logger;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleSheet;

import com.gargoylesoftware.htmlunit.Cache;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlLink;
import com.gargoylesoftware.htmlunit.html.HtmlStyle;
import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;

/**
 * <p>An ordered list of stylesheets, accessible via <tt>document.styleSheets</tt>, as specified by the
 * <a href="http://www.w3.org/TR/DOM-Level-2-Style/stylesheets.html#StyleSheets-StyleSheetList">DOM
 * Level 2 Style spec</a> and the <a href="http://developer.mozilla.org/en/docs/DOM:document.styleSheets">Gecko
 * DOM Guide</a>.</p>
 *
 * <p>If CSS is disabled via {@link com.gargoylesoftware.htmlunit.WebClient#setCssEnabled(boolean)}, instances
 * of this class will always be empty. This allows us to check for CSS enablement/disablement in a single
 * location, without having to sprinkle checks throughout the code.</p>
 *
 * @version $Revision: 3174 $
 * @author Daniel Gredler
 * @author Ahmed Ashour
 */
public class StyleSheetList extends SimpleScriptable {

    private static final long serialVersionUID = -8607630805490604483L;

    /**
     * We back the stylesheet list with an {@link HTMLCollection} of styles/links because this list
     * must be "live".
     */
    private HTMLCollection nodes_;

    /**
     * Rhino requires default constructors.
     */
    public StyleSheetList() {
        // Empty.
    }

    /**
     * Creates a new style sheet list owned by the specified document.
     *
     * @param document the owning document
     */
    public StyleSheetList(final HTMLDocument document) {
        setParentScope(document);
        setPrototype(getPrototype(getClass()));

        nodes_ = new HTMLCollection(document);

        final boolean cssEnabled = getWindow().getWebWindow().getWebClient().isCssEnabled();
        if (cssEnabled) {
            nodes_.init(document.getHtmlPage(), ".//style | .//link[lower-case(@rel)='stylesheet']");
        }
    }

    /**
     * Returns the list's length.
     *
     * @return the list's length
     */
    public int jsxGet_length() {
        return nodes_.jsxGet_length();
    }

    /**
     * Returns the style sheet at the specified index.
     *
     * @param index the index of the style sheet to return
     * @return the style sheet at the specified index
     */
    public Stylesheet jsxFunction_item(final int index) {
        final Cache cache = getWindow().getWebWindow().getWebClient().getCache();

        final HTMLElement element = (HTMLElement) nodes_.jsxFunction_item(new Integer(index));
        final DomNode node = element.getDomNodeOrDie();

        Stylesheet sheet;
        if (node instanceof HtmlStyle) {
            // <style type="text/css"> ... </style>
            final HtmlStyle style = (HtmlStyle) node;
            String css = "";
            if (style.getFirstChild() != null) {
                css = style.getFirstChild().asText();
            }
            final CSSStyleSheet cached = cache.getCachedStyleSheet(css);
            if (cached != null) {
                sheet = new Stylesheet(element, cached);
            }
            else {
                final InputSource source = new InputSource(new StringReader(css));
                sheet = new Stylesheet(element, source);
                cache.cache(css, sheet.getWrappedSheet());
            }
        }
        else {
            // <link rel="stylesheet" type="text/css" href="..." />
            final HtmlLink link = (HtmlLink) node;
            try {
                final WebResponse response = link.getWebResponse(true);
                String css = response.getContentAsString();
                final CSSStyleSheet cached = cache.getCachedStyleSheet(css);
                if (cached != null) {
                    sheet = new Stylesheet(element, cached);
                }
                else {
                    if(response.getStatusCode()>=400) {
                        // recover by parsing an empty stylesheet.
                        // workaround for http://sourceforge.net/tracker/index.php?func=detail&aid=2070940&group_id=47038&atid=448266
                        LOGGER.warning("Stylesheet reference to "+link.getHrefAttribute()+" in "+link.getOwnerDocument().getDocumentURI()+" resulted in "+response.getStatusCode());
                        css = "";
                    }
                    final InputSource source = new InputSource(new StringReader(css));
                    source.setURI(response.getUrl().toExternalForm());
                    sheet = new Stylesheet(element, source);
                    cache.cache(css, sheet.getWrappedSheet());
                }
            }
            catch (final Exception e) {
                throw Context.reportRuntimeError("Exception: " + e);
            }
        }

        return sheet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final int index, final Scriptable start) {
        if (this == start) {
            return jsxFunction_item(index);
        }
        return super.get(index, start);
    }

    private static final Logger LOGGER = Logger.getLogger(StyleSheetList.class.getName());
}
