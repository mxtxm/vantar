<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 */

//namespace arta;

/**
 * Interface for the Artaengine Inspector class.
 *
 * @author Mehdi Torabi <mehdi @ artaengine.com>
 */
interface IInspect {

    /**
     * Dump variable(s) as string.
     *
     * @param  array vars Array of variables to be dumped
     * @param  bool  browseObjects true=expand objects
     * @param  bool  print=true    true=print the dump and die, false=return the dump
     * @return string Dump As HTML or text
     */
    static public function dump($vars, $browseObjects=false, $print=true);

    /**
     * Dump variable(s) as unformatted text.
     *
     * @param  array vars Array of variables to be dumped
     * @param  bool  browseObjects true=expand objects
     * @param  bool  print=true    true=print the dump and die, false=return the dump
     * @return string Text dump
     */
    static public function dumpText($vars, $browseObjects=false, $print=true);

    /**
     * Dump variable(s) as HTML.
     *
     * @param  array vars Array of variables to be dumped
     * @param  bool  browseObjects true=expand objects
     * @param  bool  print=true    true=print the dump and die, false=return the dump
     * @return string HTML dump
     */
    static public function dumpHtml($vars, $browseObjects=false, $print=true);

    /**
     * Inspect database message renderer.
     *
     * @param  array  vars       ['Last query': [SQL, params], 'SQL queries': [queus,], 'Active queue': (string), 'Last commit': (string)]
     * @param  bool   print=true true=print the dump and die, false=return the dump
     * @return string HTML dump
     */
    static public function dumpDB($vars, $print=true);

    /**
     * Dump/render exceptions.
     *
     * @param  ErrorException err Object to be dumped
     * @param  string title       Title of the error message
     * @param  bool   die=true    true=exit after printing message
     * @param  bool   print=true  Print the dump or return it
     * @return string Dump of exception
     */
    static public function dumpException($err, $title=null, $die=true, $print=true);
}
