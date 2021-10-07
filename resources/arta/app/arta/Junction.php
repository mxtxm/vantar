<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2011::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7710
 * Created  2011/08/20
 * Updated  2011/08/20
 */

//namespace arta;

require_once 'interface/IJunction.php';

/**
 * A class that contains extra data for a relation between to IModel instances is
 * called a junction class and must inherit this class.</br>
 * IModel classes that have data properties which are arrays of other models,
 * sometimes it is required to store extra data with each array item, a junction
 * class is for this reason. The name of a junction* class should be a combination
 * of the two model names e.g. "Model1_Model2" where Model1 or Model2 or both 
 * contain an array data property of the other model.
 *
 * @copyright  ::COPYRIGHT2011::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.0.0
 * @link       http://artaengine.com/api/Junction
 * @example    http://artaengine.com/examples/models
 */
class Junction implements IJunction {

    /**
     * Junction constructor.
     */
    public function __construct() {
        $m1 = array();
        foreach (get_object_vars($this) as $col => $def) {
            if (substr_count($col, '__') < 2) {
                $m1[$col] = $def;
            }
        }
        $this->__def__[0] = $m1;
        $this->reset();
    }

    /**
     * Reset junction, empty data and set things like the object is first initialized.
     *
     * @return IJunction for method chaining
     */
    public function reset() {
        foreach ($this->__def__[0] as $col => $def) {
            $this->$col = null;
        }
        return $this;
    }

    /**
     * A dictionary of junction data properties and their value.
     *
     * @return array {property-name:value,}
     */
    public function values() {
        $vals = array();
        foreach ($this->__def__[0] as $col => $def) {
            $vals[$col] = $this->$col;
        }
        return $vals;
    }

    /**
     * A list of junction data properties.
     *
     * @return array [property-name,]
     */
    public function properties() {
        return array_keys($this->__def__[0]);
    }

    /**
     * A dictionary of all junction data properties and their definitions.
     *
     * @return array {property-name:{property-definition},}
     */
    public function cols() {
        return $this->__def__[0];
    }
}
