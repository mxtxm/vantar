<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7002
 * Created  2009/09/27
 * Updated  2012/05/25
 */

//namespace arta;

require_once 'interface/IInspect.php';

if (!function_exists('_')) { function _($string) { return $string; } }

/**
 * Methods for inspecting/debugging variables and viewing errors and exceptions.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.3
 * @since      1.0.0
 * @link       http://artaengine.com/api/Inspect
 * @example    http://artaengine.com/examples/basics
 */
class Inspect implements IInspect {

    /**
     * Dump variable(s) as string. HTML or text output depends on "Arta::$htmlDebug"
     * Use the shortcut global functions "I(var1, var2, ...);" and "inspect(var1, var2, ...);"
     * to dump variables instead of this method. When debugging an object "inspect()"
     * displays all properties and methods but "I()" does not expand the object
     * however "I()" displays sophisticated info about instances of "IModel",
     * "IRequest", "IResponse", "IDbAbstract" and "ErrorException".
     *
     * @param array $vars Array of variables to be dumped
     * @param bool  $browseObjects true=expand objects
     * @param bool  $print=true    true=print the dump and die, false=return the dump
     * @return string Dump As HTML or text
     */
    static public function dump($vars, $browseObjects=false, $print=true) {
        return Arta::$htmlDebug?
            self::dumpHtml($vars, $browseObjects, $print):
            self::dumpText($vars, $browseObjects, $print);
    }


    static private function __dumpObj($var) {
        $output['object-type'] = get_class($var);

        if ($var instanceof IModel) {
            $output['var'] = Binder::pull($var, array('full' => true));
        } elseif ($var instanceof IRequest) {
            $output['var'] = array(
                'get'      => isset($_GET)?     $_GET:     'n/a',
                'post'     => isset($_POST)?    $_POST:    'n/a',
                'files'    => isset($_FILES)?   $_FILES:   'n/a',
                'session'  => isset($_SESSION)? $_SESSION: 'n/a',
                'cookie'   => isset($_COOKIE)?  $_COOKIE:  'n/a',
                'server'   => isset($_SERVER)?  $_SERVER:  'n/a',
                'BASE_URL' => BASE_URL,
            );
        } elseif ($var instanceof IResponse) {
            $output['var'] = $var->inspect(false, false);
        } elseif ($var instanceof IDbAbstract) {
            $output['var'] = $var->inspect(false, false);
        } elseif ($var instanceof Exception) {
            $output['var'] = array(
                'class'       => get_class($var),
                'file'        => $var->getFile(),
                'line'        => $var->getLine(),
                'error'       => $var->getCode(),
                'description' => $var->getMessage(),
                'trace'       => $var->getTraceAsString(),
            );
        } elseif ($var instanceof UploadedFile) {
            $output['var'] = array(
                'name'  => $var->name,
                'type'  => $var->type,
                'temp'  => $var->temp,
                'size'  => $var->size,
                'error' => $var->error,
            );
        } elseif (method_exists($var, '__toString')) {
            $output['var'] = (string) $var->__toString();
        } else {
            $output['var'] = get_object_vars($var);
        }

        return $output;
    }


    static private function __dumpTextExpand($var, $browseObjects, $key=null, $indent='') {
        $objectType = null;

        if (!$browseObjects && is_object($var)) {
            extract(self::__dumpObj($var));
        }

        $res = '';

        if (is_null($var))
            $res = "$key: null";
        elseif (is_int($var))
            $res =  "$key: (int) $var";
        elseif (is_float($var))
            $res =  "$key: (float) $var";
        elseif (is_bool($var))
            $res =  "$key: (float) ".($var? 'true': 'false');
        elseif (is_string($var))
            $res =  "$key: (string) \"$var\"";
        elseif (is_resource($var))
            $res = "$key: (resource)";
        elseif (is_object($var)) {
            $res = array("$key: (object: instance of ".get_class($var).') (',);
            if ($browseObjects || $var instanceof stdClass) {
                foreach (get_object_vars($var) as $k => $v) {
                    $res[] = self::__dumpTextExpand($v, $browseObjects, $k, "$indent    ");
                }
                $methods = get_class_methods($var);
                sort($methods);
                foreach ($methods as $v) {
                    $res[] = "$indent    $v (method)";
                }
            }
            $res = implode("\n", $res)."\n$indent)";
        }
        elseif (is_array($var)) {
            if ($objectType) {
                $bc   = ')';
                $res = array("$key: (object: instance of $objectType) (",);
            } else {
                $bo   = '{';
                $bc   = '}';
                if (Utils::isList($var)) {
                    $bo = '[';
                    $bc = ']';
                }
                $res = array("$key: (array) $bo",);
            }
            foreach ($var as $k => $v) {
                $res[] = self::__dumpTextExpand($v, $browseObjects, $k, "$indent    ");
            }
            $res = implode("\n", $res)."\n$indent$bc";
        }

        return $indent.$res;
    }

    /**
     * Dump variable(s) as unformatted text.
     *
     * @param  array vars Array of variables to be dumped
     * @param  bool  browseObjects true=expand objects
     * @param  bool  print=true    true=print the dump and die, false=return the dump
     * @return string Text dump
     */
    static public function dumpText($vars, $browseObjects=false, $print=true) {
        require_once 'Utils.php';
        $res = array();
        foreach ($vars as $var) {
            $res[] = self::__dumpTextExpand($var, $browseObjects, null);
        }
        $dump = implode("\n", $res)."\n";

        if ($print)
            die($dump);

        return $dump;
    }

    /**
     * Inspect vars recursive
     */
    static private function __dumpHtmlExpand($var, $browseObjects, $key=null) {
        $objectType = null;
        $s          = false;

        if (!$browseObjects && is_object($var)) {
            extract(self::__dumpObj($var));
        }
        if ($var instanceof stdClass) {
            $browseObjects = true;
        }

        $kh  = '<tr><td class="k">'.$key.'</td>';
        $kvh = $kh.'<td class="t">';
        $cv  = '</td><td class="v">';

        if (is_null($var))
            $h = $kh.'<td class="x">null</td><td></td></tr>';
        elseif (is_int($var))
            $h = $kvh.'int'.$cv.$var.'</td></tr>';
        elseif (is_float($var))
            $h = $kvh.'float'.$cv.$var.'</td></tr>';
        elseif (is_bool($var))
            $h = $kvh.'bool'.$cv.($var? 'true': 'false').'</td></tr>';
        elseif (is_string($var))
            $h = $kvh.'string'.$cv.'"'.nl2br(str_replace(' ', '&nbsp;', htmlspecialchars($var))).'"</td></tr>';
        elseif (is_resource($var))
            $h = $kh.'<td class="x">resource</td></tr>';
        elseif (is_object($var)) {
            $h = '<tr><td class="o"><span>'.$key.'</span><span>object: instance '.'of <b>'.get_class($var).'</b></span><span class="b">(</span>';
            if ($browseObjects) {
                foreach (get_object_vars($var) as $k => $v) {
                    $h .= self::__dumpHtmlExpand($v, $browseObjects, "$k:");
                }
                $methods = get_class_methods($var);
                sort($methods);
                $h .= '<table>';
                foreach ($methods as $v) {
                    $h .= "<tr><td class=\"k\">method:</td><td class=\"v\">$v</td></tr>";
                }
                $h .= '</table>';
            }
            $h .= '<span class="b">)</span></td></tr>';
        }
        elseif (is_array($var)) {
            $s = true;
            if ($objectType) {
                $name = "object: instance of <b>$objectType</b>";
                $bo   = '(';
                $bc   = ')';
                $c    = 'o';
            } else {
                $name = '';
                $bo   = '{';
                $bc   = '}';
                $c    = 'a';
                if (Utils::isList($var)) {
                    $bo = '[';
                    $bc = ']';
                }
            }
            $h = "<tr><td class=\"$c\"><span>$key</span>".
                "<span>$name</span><span class=\"b\">$bo</span>";
            foreach ($var as $k => $v) {
                $h .= self::__dumpHtmlExpand($v, $browseObjects, "$k:");
            }
            $h .= "<span class=\"b\">$bc</span></td></tr>";
        }

        return '<table'.($s? ' class="block"': '').'>'.(isset($h)? $h: '').'</table>';
    }

    /**
     * Dump variable(s) as HTML.
     *
     * @param  array vars Array of variables to be dumped
     * @param  bool  browseObjects true=expand objects
     * @param  bool  print=true    true=print the dump and die, false=return the dump
     * @return string HTML dump
     */
    static public function dumpHtml($vars, $browseObjects=false, $print=true) {
        require_once 'Utils.php';

        $html =
            '<style>'.
            '#arta-inspect * { direction: LTR!important; text-align:left; }'.
            '#arta-inspect {'.
            '  border: 1px solid #333!important;'.
            '  padding: 0!important;'.
            '  background-color: #fff;'.
            '  overflow: auto;'.
            '  height: 400px;'.
            '}'.
            '#arta-inspect table {'.
            '  color: #222!important;'.
            '  font-size: 12px!important;'.
            "  font-family: monospace, 'Courier New'!important;".
            '  direction: ltr!important;'.
            '  background-color: #fff!important;'.
            '  margin: 1px 0 0 20px!important;'.
            '  width: 100%;'.
            '}'.
            '#arta-inspect h2 {'.
            '  padding: 3px!important;'.
            '  color: #111!important;'.
            '  margin: 0 0 15px 0!important;'.
            '  font-size: 14px!important;'.
            '}'.
            '#arta-inspect table.block { margin-bottom:10px!important; }'.
            '#arta-inspect tr {'.
            '  direction: ltr!important;'.
            '}'.
            '#arta-inspect td {'.
            '  padding: 0!important;'.
            '  margin: 0!important;'.
            '  border-bottom: 1px dotted #ddd!important;'.
            '  direction: ltr!important;'.
            '  font-weight: normal!important;'.
            "  font-family: monospace, 'Courier New'!important;".
            '}'.
            '#arta-inspect td.t {'.
            '  color: #71A8DE!important;'.
            '  width: 65px!important;'.
            '  text-align: center!important;'.
            '}'.
            '#arta-inspect td.k {'.
            '  background-color: #FFFFC9!important;'.
            '  color: #888!important;'.
            '  width: 135px!important;'.
            '  padding: 1px!important;'.
            '  text-align: right!important;'.
            '}'.
            '#arta-inspect .b {'.
            '  font-size: 14px!important;'.
            '  font-weight: bold!important;'.
            '  background-color: #a8e1f9!important;'.
            '  padding: 0 7px!important;'.
            '}'.
            '#arta-inspect td.v {'.
            '  color: #222!important;'.
            '}'.
            '#arta-inspect td.x {'.
            '  background-color: #EFBF67!important;'.
            '  font-weight: bold!important;'.
            '  width: 65px!important;'.
            '  color: #fff!important;'.
            '  text-align: center!important;'.
            '}'.
            '#arta-inspect td.a {'.
            '  border-left: 7px solid #CCECF9!important;'.
            '}'.
            '#arta-inspect td.a span {'.
            '  background-color: #CCECF9!important;'.
            '  color: black!important;'.
            '  font-size: 13px!important;'.
            '}'.
            '#arta-inspect td.o {'.
            '  border-left: 7px solid #ECECF9!important;'.
            '}'.
            '#arta-inspect td.o > span {'.
            '  background-color: #ECECF9!important;'.
            '  color: black!important;'.
            '  font-size: 13px!important;'.
            '}'.
            '</style>';

        $html .= '<div id="arta-inspect"><h2>Artaengine inspect</h2>';
        foreach ($vars as $var) {
            $html .= self::__dumpHtmlExpand($var, $browseObjects, null);
        }
        $html .= '</div>';

        if ($print)
            die($html);

        return $html;
    }

    /**
     * Recursively flatten a list to string. [v1, v2, [v3, v4],] ==> '(v1, v2, (v3, v4))'
     *
     * @param  array vars  Array to be flattened (keys are ignored)
     * @return string The flattened array
     */
    static public function flattenList($vars) {
        if (is_array($vars)) {   
            $str = '';
            foreach ($vars as $val) {
                $str .= ($str? ', ': '').self::flattenList($val);
            }
            return "($str)";
        }
        return $vars;
    }

    /**
     * Recursively flatten a dictionary to string. {k1: v1, k2: v1, k3 {k4: v4},} ==> '(k1: v1, k2: v1, k2: (k4: v4))'
     *
     * @param  array vars  Array to be flattened
     * @return string The flattened array
     */
    static public function flattenDict($vars) {
        if (is_array($vars)) {   
            $str = '';
            foreach ($vars as $key => $val) {
                $str .= ($str? ', ': '')."$key: ".(is_array($val)? self::flattenDict($val): $val);
            }
            return "($str)";
        }
        return $vars;
    }

    /**
     * Inspect database message renderer. This method is used internally by instances
     * or "IDBabstract" "inspect()" method to render messages. HTML or text output depends on "Arta::$htmlDebug".
     *
     * @param  array  vars       ['Last query': [SQL, params], 'SQL queries': [queus,], 'Active queue': (string), 'Last commit': (string)]
     * @param  bool   print=true true=print the dump and die, false=return the dump
     * @return string HTML dump
     */
    static public function dumpDB($vars, $print=true) {
        if (Arta::$htmlDebug) {
            $output =
                '<style>'.
                '#arta-inspect * { direction: ltr!important; }'.
                '#arta-inspect {border: 1px solid #333!important; padding: 5px!important;'.
                'overflow: auto; background-color:#fff}'.
                '#arta-inspect h3 { color: #71A8DE; font-size: 12px; padding: 0 5px!important;'.
                'font-weight: normal!important; }'.
                '#arta-inspect p {background-color:#fff; color: #222; font-size: 12px!important;'.
                "font-family: monospace, 'Courier New'!important;".
                'margin: 2px 30px!important; padding: 0px!important;'.
                'font-weight: normal!important; border-bottom: 0px solid #aaa!important; }'.
                '#arta-inspect p.e {color: #BB0F0F;}'.
                '#arta-inspect p.t {background-color: #EFBF67!important;'.
                'font-weight: bold!important; width: 100px!important; color: #000!important;'.
                'text-align: center!important; }'.
                '</style>';
            /* Last executed query */
            //$vars = array_values($vars);
            $output .=
                '<h3>'._('Last executed SQL').'</h3><p>'.
                nl2br(str_replace(';', ";\n", htmlspecialchars($vars['Last query'][0]))).'</p>'.
                '<h3>'._('Parameters').'</h3><p>'.
                ($vars['Last query'][1]? htmlspecialchars(self::flattenList($vars['Last query'][1])): '<p class="t">None</p>').'</p>';
            /* Last executed transaction */
            $output .= '<h3>'._('Last committed queue/transaction').'</h3>';
            if ($vars['Last commit']) {
                foreach ($vars['Last commit'] as $query) {
                    $output .= '<p>'.htmlspecialchars($query).'</p>';
                }
            } else {
                $output .= '<p class="t">None</p>';
            }
            /* Transaction que */
            $output .= '<h3>'._('SQL queues')."</h3>";
            if ($vars['SQL queues']) {
                foreach ($vars['SQL queues'] as $queName => $que) {
                    $output .= '<p class="t">'.htmlspecialchars($queName).'</p>';
                    foreach ($que as $query)
                        $output .= '<p>'.htmlspecialchars($query).'</p>';
                    $output .= '';
                }
            } else {
                $output .= '<p class="t">None</p>';
            }

            $output .= '<h3>'._('Active queue').'</h3><p class="t">'.htmlspecialchars($vars['Active queue']).'</p>';
            /* connection */
            if ($vars['Connection']) {
                $output .= '<h3>'._('Connection').'</h3>';
                foreach ($vars['Connection'] as $k => $v) {
                    $output .= "<p>$k: $v</p>";
                }
            }
            /* error */
            if ($vars['Error']) {
                $output .= '<h3>'._('Error').'</h3><p class="e">'.htmlspecialchars($vars['Error']).'</p>';
            }

            $output = '<div id="arta-inspect">'.$output.'</div>';
        } else {
            $output =
                "----------------------------------------------------------------------\n".
                "Artaengine inspect\n".
                "------------------------------------\n".
                _('Last executed SQL')."\n".
                str_replace(';', ";\n", $vars['Last query'][0])."\n\n".
                _('Parameters').":\n".
                ($vars['Last query'][1]? self::flattenList($vars['Last query'][1]): '')."\n";
            /* Last executed transaction */
            $output .=
                "------------------------------------\n".
                _('Last committed queue/transaction').":\n";

            if ($vars['Last commit']) {
                foreach ($vars['Last commit'] as $query) {
                    $output .= "\t$query\n";
                }
            }
            /* Transaction  que */
            $output .=
                "------------------------------------\n".
                _('SQL queues').":\n";

            if ($vars['SQL queues'])
                foreach ($vars['SQL queues'] as $queName => $que) {
                    $output .= "\t$queName\n";
                    foreach ($que as $query) {
                        $output .= "\t\t$query\n";
                    }
                    $output .= '';
                }
            $output .= "\n"._('Active queue').': '.$vars['Active queue']."\n";
            /* connection settings */
            $output .=
                "------------------------------------\n";
            /* connection settings */
            if ($vars['Connection']) {
                $output .= _('Connection').":\n";
                foreach ($vars['Connection'] as $k => $v) {
                    $output .= "\t$k: $v\n";
                }
            }
            /* error message */
            if ($vars['Error']) {
                $output .= "\n"._('Error').': '.$vars['Error']."\n";
            }

            $output .=
                "----------------------------------------------------------------------\n";
        }
        /* * */
        if ($print)
            die($output);

        return $output;
    }

    /**
     * Dump/render exceptions. HTML or text output depends on "Arta::$htmlDebug".
     *
     * @param  ErrorException err Object to be dumped
     * @param  string title       Title of the error message
     * @param  bool   die=true    true=exit after printing message
     * @param  bool   print=true  Print the dump or return it
     * @return string Dump of exception
     */
    static public function dumpException($err, $title=null, $die=true, $print=true) {
        $file = $line = $code = $msg2  = $trace = '';
        if (!$title)
            $title = _('Artaengine error handler');

        $hint = _('The following error was caught by Artaengine error handler');
        /* if buildout error */
        $request = Arta::g('request');
        if (  $request &&
              $request->path(0) === 'build'
           && isset(Arta::$configs['arta']['build'])
           && Arta::$configs['arta']['build']) {
            $hint = sprintf(
                _("Please check '%s'. Is it writable? If it exists please delete all files inside it and try again."),
                ARTA_TEMP_DIR
            );
        }

        if (is_object($err)) {
            $type  = get_class($err);
            $file  = $err->getFile();
            $line  = $err->getLine();
            $code  = $err->getCode();
            $msg1  = $err->getMessage();
            $msg2  = '';
            $trace = $err->getTraceAsString();
        } elseif (is_array($err)) {
            $type  = isset($err['type'])?  $err['type']:  '';
            $file  = isset($err['file'])?  $err['file']:  '';
            $line  = isset($err['line'])?  $err['line']:  '';
            $code  = isset($err['code'])?  $err['code']:  '';
            $msg1  = isset($err['msg1'])?  $err['msg1']:  '';
            $msg2  = isset($err['msg2'])?  $err['msg2']:  '';
            $trace = isset($err['trace'])? $err['trace']: '';
        } else {
            $msg1  = $err;
        }

        if (Arta::$htmlDebug) {
            $title = htmlspecialchars($title);
            $file  = htmlspecialchars($file);
            $line  = htmlspecialchars($line);
            $code  = htmlspecialchars($code);
            $msg1  = $type === 'DatabaseError'? $msg1: htmlspecialchars($msg1);
            $msg2  = htmlspecialchars($msg2);
            $trace = htmlspecialchars($trace);

            $output =
                '<style>'.
                '#arta-inspect * { direction: ltr!important; }'.
                '#arta-inspect {background-color: #fff; padding: 10px; border: 5px dashed #FABABA; }'.
                '#arta-inspect h1 {background-color: #fff; font-size: 16px; color: black; padding: 0 0 5px   0; margin: 0;}'.
                '#arta-inspect h2 {background-color: #fff; font-size: 14px; color: #555; }'.
                '#arta-inspect table {background-color: #fff; border-collapse:collapse; margin: 10px 0; '.
                'border-top: 2px solid red; }'.
                '#arta-inspect td {background-color: #fff; padding: 5px; font-size: 12px; border-bottom: 1px '.
                'solid #eee; font-size: 12px; color: #333;}'.
                '#arta-inspect td.c { text-align: right; width: 100px; background-color: '.
                '#f8f8f8; }'.
                '#arta-inspect div.separator {background-color: #fff; padding-top: 6px; margin-top: 6px; '.
                'border-top: 1px solid #ddd; }'.
                '#arta-inspect #t td.c { background-color: #f2f2f2; }'.
                '#arta-inspect #t td { background-color: #f6f6f6; color: #777; '.
                'font-size: 11px; }'.
                '</style>'.
                '<div id="arta-inspect">'.'<h1>'.$title.'</h1><h2>'.$hint.
                '</h2><table>'.
                '<tr><td class="c">Type</td><td>'.$type.'</td></tr>'.
                '<tr><td class="c">File</td><td>'.$file.'</td></tr>';

            if ($line) {
                $output .= '<tr><td class="c">Line</td><td>'.$line.'</td></tr>';
            }
            if ($code) {
                $output .= '<tr><td class="c">Error code</td><td>'.$code.'</td></tr>';
            }

            $output .= '<tr><td class="c">Message</td><td>'.
                ($msg1? '<p>'.nl2br($msg1).'</p>': '').
                ($msg2? '<p>'.nl2br($msg2).'</p>': '').
                '</td></tr>';

            if ($trace) {
                $output .= '<tr id="t"><td class="c">Trace</td><td>'.str_replace("\n", '<div class="separator"></div>', $trace).'</td></tr>';
            }

            $output .= '</table></div>';

        } else {
            $output =
                "----------------------------------------------------------------------\n".
                $title."\n".
                $hint."\n\n".
                "Exception': $type\n";
                "File: $file\n";

            if ($line) {
                $output .= "Line: $line\n";
            }
            if ($code) {
                $output .= "Error code: $code\n";
            }

            $output .= "Message: \n$msg1\n$msg2\n";

            if ($trace) {
                $output .= "Trace:\n".$trace."\n";
            }
            $output .=
                "----------------------------------------------------------------------\n";
        }
        /* * */
        if ($print) {
            echo $output;
        } else {
            return $output;
        }
        if ($die) {
            die();
        }
    }

    public function __toString() {
        return '[arta/Inspect instance: dumps variables as HTML or text]';
    }
}
