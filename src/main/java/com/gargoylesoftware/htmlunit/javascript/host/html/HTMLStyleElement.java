/*
 * Copyright (c) 2002-2009 Gargoyle Software Inc.
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
package com.gargoylesoftware.htmlunit.javascript.host.html;

import java.io.StringReader;

import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleSheet;

import com.gargoylesoftware.htmlunit.Cache;
import com.gargoylesoftware.htmlunit.html.HtmlStyle;
import com.gargoylesoftware.htmlunit.javascript.host.Stylesheet;

/**
 * The JavaScript object "HTMLStyleElement".
 *
 * @version $Revision: 4859 $
 * @author Ahmed Ashour
 * @author Marc Guillemot
 */
public class HTMLStyleElement extends HTMLElement {

    private static final long serialVersionUID = 944381786297995169L;

    private Stylesheet sheet_;

    /**
     * Creates an instance.
     */
    public HTMLStyleElement() {
        // Empty.
    }

    /**
     * Gets the associated sheet.
     * @see <a href="http://www.xulplanet.com/references/objref/HTMLStyleElement.html">Mozilla doc</a>
     * @return the sheet
     */
    public Stylesheet jsxGet_sheet() {
        if (sheet_ != null) {
            return sheet_;
        }

        String css = "";
        final HtmlStyle style = (HtmlStyle) getDomNodeOrDie();
        if (style.getFirstChild() != null) {
            css = style.getFirstChild().asText();
        }

        final Cache cache = getWindow().getWebWindow().getWebClient().getCache();
        final CSSStyleSheet cached = cache.getCachedStyleSheet(css);
        final String uri = getDomNodeOrDie().getPage().getWebResponse().getRequestSettings()
        .getUrl().toExternalForm();
        if (cached != null) {
            sheet_ = new Stylesheet(this, cached, uri);
        }
        else {
            final InputSource source = new InputSource(new StringReader(css));
            sheet_ = new Stylesheet(this, source, uri);
            cache.cache(css, sheet_.getWrappedSheet());
        }

        return sheet_;
    }

    /**
     * Gets the associated sheet (IE).
     * @return the sheet
     */
    public Stylesheet jsxGet_styleSheet() {
        return jsxGet_sheet();
    }
}
