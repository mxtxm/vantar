package com.vantar.util.string;

import java.util.HashMap;
import java.util.Map;

/**
 * HtmlEscape escape utilities
 */
public class HtmlEscape {

    private final Map<Character, String> htmlSpecialChars;


    public HtmlEscape() {
        htmlSpecialChars = new HashMap<>(260, 1);
        htmlSpecialChars.put('\u0026', "&amp;");
        htmlSpecialChars.put('\u003C', "&lt;");
        htmlSpecialChars.put('\u003E', "&gt;");
        htmlSpecialChars.put('\u0022', "&quot;");

        htmlSpecialChars.put('\u0152', "&OElig;");
        htmlSpecialChars.put('\u0153', "&oelig;");
        htmlSpecialChars.put('\u0160', "&Scaron;");
        htmlSpecialChars.put('\u0161', "&scaron;");
        htmlSpecialChars.put('\u0178', "&Yuml;");
        htmlSpecialChars.put('\u02C6', "&circ;");
        htmlSpecialChars.put('\u02DC', "&tilde;");
        htmlSpecialChars.put('\u2002', "&ensp;");
        htmlSpecialChars.put('\u2003', "&emsp;");
        htmlSpecialChars.put('\u2009', "&thinsp;");
        htmlSpecialChars.put('\u200C', "&zwnj;");
        htmlSpecialChars.put('\u200D', "&zwj;");
        htmlSpecialChars.put('\u200E', "&lrm;");
        htmlSpecialChars.put('\u200F', "&rlm;");
        htmlSpecialChars.put('\u2013', "&ndash;");
        htmlSpecialChars.put('\u2014', "&mdash;");
        htmlSpecialChars.put('\u2018', "&lsquo;");
        htmlSpecialChars.put('\u2019', "&rsquo;");
        htmlSpecialChars.put('\u201A', "&sbquo;");
        htmlSpecialChars.put('\u201C', "&ldquo;");
        htmlSpecialChars.put('\u201D', "&rdquo;");
        htmlSpecialChars.put('\u201E', "&bdquo;");
        htmlSpecialChars.put('\u2020', "&dagger;");
        htmlSpecialChars.put('\u2021', "&Dagger;");
        htmlSpecialChars.put('\u2030', "&permil;");
        htmlSpecialChars.put('\u2039', "&lsaquo;");
        htmlSpecialChars.put('\u203A', "&rsaquo;");
        htmlSpecialChars.put('\u20AC', "&euro;");
        // Character entity references for ISO 8859-1 characters
        htmlSpecialChars.put('\u00A0', "&nbsp;");
        htmlSpecialChars.put('\u00A1', "&iexcl;");
        htmlSpecialChars.put('\u00A2', "&cent;");
        htmlSpecialChars.put('\u00A3', "&pound;");
        htmlSpecialChars.put('\u00A4', "&curren;");
        htmlSpecialChars.put('\u00A5', "&yen;");
        htmlSpecialChars.put('\u00A6', "&brvbar;");
        htmlSpecialChars.put('\u00A7', "&sect;");
        htmlSpecialChars.put('\u00A8', "&uml;");
        htmlSpecialChars.put('\u00A9', "&copy;");
        htmlSpecialChars.put('\u00AA', "&ordf;");
        htmlSpecialChars.put('\u00AB', "&laquo;");
        htmlSpecialChars.put('\u00AC', "&not;");
        htmlSpecialChars.put('\u00AD', "&shy;");
        htmlSpecialChars.put('\u00AE', "&reg;");
        htmlSpecialChars.put('\u00AF', "&macr;");
        htmlSpecialChars.put('\u00B0', "&deg;");
        htmlSpecialChars.put('\u00B1', "&plusmn;");
        htmlSpecialChars.put('\u00B2', "&sup2;");
        htmlSpecialChars.put('\u00B3', "&sup3;");
        htmlSpecialChars.put('\u00B4', "&acute;");
        htmlSpecialChars.put('\u00B5', "&micro;");
        htmlSpecialChars.put('\u00B6', "&para;");
        htmlSpecialChars.put('\u00B7', "&middot;");
        htmlSpecialChars.put('\u00B8', "&cedil;");
        htmlSpecialChars.put('\u00B9', "&sup1;");
        htmlSpecialChars.put('\u00BA', "&ordm;");
        htmlSpecialChars.put('\u00BB', "&raquo;");
        htmlSpecialChars.put('\u00BC', "&frac14;");
        htmlSpecialChars.put('\u00BD', "&frac12;");
        htmlSpecialChars.put('\u00BE', "&frac34;");
        htmlSpecialChars.put('\u00BF', "&iquest;");
        htmlSpecialChars.put('\u00C0', "&Agrave;");
        htmlSpecialChars.put('\u00C1', "&Aacute;");
        htmlSpecialChars.put('\u00C2', "&Acirc;");
        htmlSpecialChars.put('\u00C3', "&Atilde;");
        htmlSpecialChars.put('\u00C4', "&Auml;");
        htmlSpecialChars.put('\u00C5', "&Aring;");
        htmlSpecialChars.put('\u00C6', "&AElig;");
        htmlSpecialChars.put('\u00C7', "&Ccedil;");
        htmlSpecialChars.put('\u00C8', "&Egrave;");
        htmlSpecialChars.put('\u00C9', "&Eacute;");
        htmlSpecialChars.put('\u00CA', "&Ecirc;");
        htmlSpecialChars.put('\u00CB', "&Euml;");
        htmlSpecialChars.put('\u00CC', "&Igrave;");
        htmlSpecialChars.put('\u00CD', "&Iacute;");
        htmlSpecialChars.put('\u00CE', "&Icirc;");
        htmlSpecialChars.put('\u00CF', "&Iuml;");
        htmlSpecialChars.put('\u00D0', "&ETH;");
        htmlSpecialChars.put('\u00D1', "&Ntilde;");
        htmlSpecialChars.put('\u00D2', "&Ograve;");
        htmlSpecialChars.put('\u00D3', "&Oacute;");
        htmlSpecialChars.put('\u00D4', "&Ocirc;");
        htmlSpecialChars.put('\u00D5', "&Otilde;");
        htmlSpecialChars.put('\u00D6', "&Ouml;");
        htmlSpecialChars.put('\u00D7', "&times;");
        htmlSpecialChars.put('\u00D8', "&Oslash;");
        htmlSpecialChars.put('\u00D9', "&Ugrave;");
        htmlSpecialChars.put('\u00DA', "&Uacute;");
        htmlSpecialChars.put('\u00DB', "&Ucirc;");
        htmlSpecialChars.put('\u00DC', "&Uuml;");
        htmlSpecialChars.put('\u00DD', "&Yacute;");
        htmlSpecialChars.put('\u00DE', "&THORN;");
        htmlSpecialChars.put('\u00DF', "&szlig;");
        htmlSpecialChars.put('\u00E0', "&agrave;");
        htmlSpecialChars.put('\u00E1', "&aacute;");
        htmlSpecialChars.put('\u00E2', "&acirc;");
        htmlSpecialChars.put('\u00E3', "&atilde;");
        htmlSpecialChars.put('\u00E4', "&auml;");
        htmlSpecialChars.put('\u00E5', "&aring;");
        htmlSpecialChars.put('\u00E6', "&aelig;");
        htmlSpecialChars.put('\u00E7', "&ccedil;");
        htmlSpecialChars.put('\u00E8', "&egrave;");
        htmlSpecialChars.put('\u00E9', "&eacute;");
        htmlSpecialChars.put('\u00EA', "&ecirc;");
        htmlSpecialChars.put('\u00EB', "&euml;");
        htmlSpecialChars.put('\u00EC', "&igrave;");
        htmlSpecialChars.put('\u00ED', "&iacute;");
        htmlSpecialChars.put('\u00EE', "&icirc;");
        htmlSpecialChars.put('\u00EF', "&iuml;");
        htmlSpecialChars.put('\u00F0', "&eth;");
        htmlSpecialChars.put('\u00F1', "&ntilde;");
        htmlSpecialChars.put('\u00F2', "&ograve;");
        htmlSpecialChars.put('\u00F3', "&oacute;");
        htmlSpecialChars.put('\u00F4', "&ocirc;");
        htmlSpecialChars.put('\u00F5', "&otilde;");
        htmlSpecialChars.put('\u00F6', "&ouml;");
        htmlSpecialChars.put('\u00F7', "&divide;");
        htmlSpecialChars.put('\u00F8', "&oslash;");
        htmlSpecialChars.put('\u00F9', "&ugrave;");
        htmlSpecialChars.put('\u00FA', "&uacute;");
        htmlSpecialChars.put('\u00FB', "&ucirc;");
        htmlSpecialChars.put('\u00FC', "&uuml;");
        htmlSpecialChars.put('\u00FD', "&yacute;");
        htmlSpecialChars.put('\u00FE', "&thorn;");
        htmlSpecialChars.put('\u00FF', "&yuml;");
        // Mathematical, Greek and Symbolic characters for HTML
        htmlSpecialChars.put('\u0192', "&fnof;");
        htmlSpecialChars.put('\u0391', "&Alpha;");
        htmlSpecialChars.put('\u0392', "&Beta;");
        htmlSpecialChars.put('\u0393', "&Gamma;");
        htmlSpecialChars.put('\u0394', "&Delta;");
        htmlSpecialChars.put('\u0395', "&Epsilon;");
        htmlSpecialChars.put('\u0396', "&Zeta;");
        htmlSpecialChars.put('\u0397', "&Eta;");
        htmlSpecialChars.put('\u0398', "&Theta;");
        htmlSpecialChars.put('\u0399', "&Iota;");
        htmlSpecialChars.put('\u039A', "&Kappa;");
        htmlSpecialChars.put('\u039B', "&Lambda;");
        htmlSpecialChars.put('\u039C', "&Mu;");
        htmlSpecialChars.put('\u039D', "&Nu;");
        htmlSpecialChars.put('\u039E', "&Xi;");
        htmlSpecialChars.put('\u039F', "&Omicron;");
        htmlSpecialChars.put('\u03A0', "&Pi;");
        htmlSpecialChars.put('\u03A1', "&Rho;");
        htmlSpecialChars.put('\u03A3', "&Sigma;");
        htmlSpecialChars.put('\u03A4', "&Tau;");
        htmlSpecialChars.put('\u03A5', "&Upsilon;");
        htmlSpecialChars.put('\u03A6', "&Phi;");
        htmlSpecialChars.put('\u03A7', "&Chi;");
        htmlSpecialChars.put('\u03A8', "&Psi;");
        htmlSpecialChars.put('\u03A9', "&Omega;");
        htmlSpecialChars.put('\u03B1', "&alpha;");
        htmlSpecialChars.put('\u03B2', "&beta;");
        htmlSpecialChars.put('\u03B3', "&gamma;");
        htmlSpecialChars.put('\u03B4', "&delta;");
        htmlSpecialChars.put('\u03B5', "&epsilon;");
        htmlSpecialChars.put('\u03B6', "&zeta;");
        htmlSpecialChars.put('\u03B7', "&eta;");
        htmlSpecialChars.put('\u03B8', "&theta;");
        htmlSpecialChars.put('\u03B9', "&iota;");
        htmlSpecialChars.put('\u03BA', "&kappa;");
        htmlSpecialChars.put('\u03BB', "&lambda;");
        htmlSpecialChars.put('\u03BC', "&mu;");
        htmlSpecialChars.put('\u03BD', "&nu;");
        htmlSpecialChars.put('\u03BE', "&xi;");
        htmlSpecialChars.put('\u03BF', "&omicron;");
        htmlSpecialChars.put('\u03C0', "&pi;");
        htmlSpecialChars.put('\u03C1', "&rho;");
        htmlSpecialChars.put('\u03C2', "&sigmaf;");
        htmlSpecialChars.put('\u03C3', "&sigma;");
        htmlSpecialChars.put('\u03C4', "&tau;");
        htmlSpecialChars.put('\u03C5', "&upsilon;");
        htmlSpecialChars.put('\u03C6', "&phi;");
        htmlSpecialChars.put('\u03C7', "&chi;");
        htmlSpecialChars.put('\u03C8', "&psi;");
        htmlSpecialChars.put('\u03C9', "&omega;");
        htmlSpecialChars.put('\u03D1', "&thetasym;");
        htmlSpecialChars.put('\u03D2', "&upsih;");
        htmlSpecialChars.put('\u03D6', "&piv;");
        htmlSpecialChars.put('\u2022', "&bull;");
        htmlSpecialChars.put('\u2026', "&hellip;");
        htmlSpecialChars.put('\u2032', "&prime;");
        htmlSpecialChars.put('\u2033', "&Prime;");
        htmlSpecialChars.put('\u203E', "&oline;");
        htmlSpecialChars.put('\u2044', "&frasl;");
        htmlSpecialChars.put('\u2118', "&weierp;");
        htmlSpecialChars.put('\u2111', "&image;");
        htmlSpecialChars.put('\u211C', "&real;");
        htmlSpecialChars.put('\u2122', "&trade;");
        htmlSpecialChars.put('\u2135', "&alefsym;");
        htmlSpecialChars.put('\u2190', "&larr;");
        htmlSpecialChars.put('\u2191', "&uarr;");
        htmlSpecialChars.put('\u2192', "&rarr;");
        htmlSpecialChars.put('\u2193', "&darr;");
        htmlSpecialChars.put('\u2194', "&harr;");
        htmlSpecialChars.put('\u21B5', "&crarr;");
        htmlSpecialChars.put('\u21D0', "&lArr;");
        htmlSpecialChars.put('\u21D1', "&uArr;");
        htmlSpecialChars.put('\u21D2', "&rArr;");
        htmlSpecialChars.put('\u21D3', "&dArr;");
        htmlSpecialChars.put('\u21D4', "&hArr;");
        htmlSpecialChars.put('\u2200', "&forall;");
        htmlSpecialChars.put('\u2202', "&part;");
        htmlSpecialChars.put('\u2203', "&exist;");
        htmlSpecialChars.put('\u2205', "&empty;");
        htmlSpecialChars.put('\u2207', "&nabla;");
        htmlSpecialChars.put('\u2208', "&isin;");
        htmlSpecialChars.put('\u2209', "&notin;");
        htmlSpecialChars.put('\u220B', "&ni;");
        htmlSpecialChars.put('\u220F', "&prod;");
        htmlSpecialChars.put('\u2211', "&sum;");
        htmlSpecialChars.put('\u2212', "&minus;");
        htmlSpecialChars.put('\u2217', "&lowast;");
        htmlSpecialChars.put('\u221A', "&radic;");
        htmlSpecialChars.put('\u221D', "&prop;");
        htmlSpecialChars.put('\u221E', "&infin;");
        htmlSpecialChars.put('\u2220', "&ang;");
        htmlSpecialChars.put('\u2227', "&and;");
        htmlSpecialChars.put('\u2228', "&or;");
        htmlSpecialChars.put('\u2229', "&cap;");
        htmlSpecialChars.put('\u222A', "&cup;");
        htmlSpecialChars.put('\u222B', "&int;");
        htmlSpecialChars.put('\u2234', "&there4;");
        htmlSpecialChars.put('\u223C', "&sim;");
        htmlSpecialChars.put('\u2245', "&cong;");
        htmlSpecialChars.put('\u2248', "&asymp;");
        htmlSpecialChars.put('\u2260', "&ne;");
        htmlSpecialChars.put('\u2261', "&equiv;");
        htmlSpecialChars.put('\u2264', "&le;");
        htmlSpecialChars.put('\u2265', "&ge;");
        htmlSpecialChars.put('\u2282', "&sub;");
        htmlSpecialChars.put('\u2283', "&sup;");
        htmlSpecialChars.put('\u2284', "&nsub;");
        htmlSpecialChars.put('\u2286', "&sube;");
        htmlSpecialChars.put('\u2287', "&supe;");
        htmlSpecialChars.put('\u2295', "&oplus;");
        htmlSpecialChars.put('\u2297', "&otimes;");
        htmlSpecialChars.put('\u22A5', "&perp;");
        htmlSpecialChars.put('\u22C5', "&sdot;");
        htmlSpecialChars.put('\u2308', "&lceil;");
        htmlSpecialChars.put('\u2309', "&rceil;");
        htmlSpecialChars.put('\u230A', "&lfloor;");
        htmlSpecialChars.put('\u230B', "&rfloor;");
        htmlSpecialChars.put('\u2329', "&lang;");
        htmlSpecialChars.put('\u232A', "&rang;");
        htmlSpecialChars.put('\u25CA', "&loz;");
        htmlSpecialChars.put('\u2660', "&spades;");
        htmlSpecialChars.put('\u2663', "&clubs;");
        htmlSpecialChars.put('\u2665', "&hearts;");
        htmlSpecialChars.put('\u2666', "&diams;");
    }

    /**
     * Encode for html
     * @param html value to encode
     * @return encoded html value
     */
    public String encode(String html) {
        if (null == html) {
            return null;
        }

        StringBuffer encodedString = null;
        char[] stringToEncodeArray = html.toCharArray();
        int lastMatch = -1;
        int difference;

        for (int i = 0; i < stringToEncodeArray.length; i++) {
            char char_to_encode = stringToEncodeArray[i];

            if (htmlSpecialChars.containsKey(char_to_encode)) {
                if (null == encodedString) {
                    encodedString = new StringBuffer(html.length());
                }
                difference = i - (lastMatch + 1);
                if (difference > 0) {
                    encodedString.append(stringToEncodeArray, lastMatch + 1, difference);
                }
                encodedString.append(htmlSpecialChars.get(char_to_encode));
                lastMatch = i;
            }
        }

        if (null == encodedString) {
            return html;
        } else {
            difference = stringToEncodeArray.length - (lastMatch + 1);
            if (difference > 0) {
                encodedString.append(stringToEncodeArray, lastMatch + 1, difference);
            }
            return encodedString.toString();
        }
    }
}
