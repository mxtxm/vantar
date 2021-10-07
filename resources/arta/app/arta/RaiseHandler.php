<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2012::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7007
 * Created  2012/08/31
 * Updated  2012/08/31
 */

//namespace arta;

/**
 * Handle events controller. This class is for Artaengine internal use only.
 * Distributes exceptions and special events to the most apropriate handler.
 * If handler is missing and the app is in DEBUG mode will display "EventHandler".
 *
 * @copyright  ::COPYRIGHT2012::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.2.1
 * @link       http://artaengine.com/api/Force
 * @example    http://artaengine.com/examples/models
 */
class RaiseHandler {

    /**
     * Call a handler. If event handler factory not exists raise and display an exception.
     *
     * @param char $type Handler/event type
     *        U=Unauthorized User Handler, I=Illegal User Handler,
     *        X=Exception Handler, F=No Factory Handler,
     *        N=URL Not Found Handler, A=Not Ajax Request Handler.
     * @params [mixed] $params Params to be passed to the handler method/function
     */
    static public function raise($type) {
        $otherParams = func_get_args();
        unset($otherParams[0]);
        $params[] = $request = Arta::$globals['request'];

        switch ($type) {

            case 'U':
                $title      = 'Unauthorized User Handler';
                $factoryDef = Arta::$unauthorizedHandler;
                $params[]   = $request->currentUrl();
                break;

            case 'I':
                $title      = 'Illegal User Handler';
                $factoryDef = Arta::$illegalHandler;
                $params[]   = $request->currentUrl();
                break;

            case 'X':
                $title      = 'Exception Handler';
                $factoryDef = Arta::$exceptionHandler;
                break;

            case 'F':
                $title      = 'No Factory Handler';
                $factoryDef = Arta::$nofactoryHandler;
                break;

            case 'N':
                $title      = 'URL Not Found Handler';
                $factoryDef = Arta::$notfoundHandler;
                $params[]   = $request->currentUrl();
                break;

            case 'A':
                $title      = 'Not Ajax Request Handler';
                $factoryDef = Arta::$notajaxHandler;
                $params[]   = $request->currentUrl();
                break;

            default:
                $title = null;
                $factoryDef = null;
        }

        if ($otherParams) {
            if (is_array($otherParams)) {
                $params = array_merge($params, $otherParams);
            } else {
                $params[] = $otherParams;
            }
        }
        /* load factory */
        if (!$factoryDef || count($factoryDef) != 4) {
            if (DEBUG) {
                Inspect::dumpException(new EventHandler(EventHandler::HANDLER_NOT_DEFINED, $title));
            }
            die;
        }
        list($facPath, $tplObjectName, $facClass, $facMethod) = $factoryDef;
        if (!file_exists($facPath)) {
            if (DEBUG) {
                Inspect::dumpException(new EventHandler(EventHandler::HANDLER_FILE_NOT_EXISTS, array($title, $facPath)));
            }
            die;
        }

        require_once 'Template.php';
        require_once 'Response.php';
        require_once 'Database.php';
        require_once $facPath;

        if (!$facClass) {
            if (!function_exists($facMethod)) {
                if (DEBUG) {
                    Inspect::dumpException(
                        new EventHandler(
                            EventHandler::HANDLER_METHOD_NOT_EXISTS,
                            array($title, $facPath, $facMethod)
                        )
                    );
                }
                die;
            }
            $params[] = $tplObjectName;
            call_user_func_array($facMethod, $params);
            die;
        }

        if (!class_exists($facClass)) {
            if (DEBUG) {
                Inspect::dumpException(
                    new EventHandler(
                        EventHandler::HANDLER_CLASS_NOT_EXISTS,
                        array($title, $facPath, $facClass)
                    )
                );
            }
            die;
        }

        Arta::$factory = $factory = new $facClass();
        if (!method_exists($factory, $facMethod)) {
            if (DEBUG) {
                Inspect::dumpException(
                    new EventHandler(
                        EventHandler::HANDLER_METHOD_NOT_EXISTS,
                        array($title, $facPath, $facMethod)
                    )
                );
            }
            die;
        }

        if (property_exists($factory, '__tpl')) {
            $factory->__tpl = $tplObjectName;
        }

        call_user_func_array(array(&$factory, $facMethod), $params);
        die;
    }
}
