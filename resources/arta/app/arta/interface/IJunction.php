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
 * A class that contains extra data for a relation between to IModel instances is
 * called a junction class and must implement this interface.</br>
 * IModel classes that have data properties which are arrays of other models,
 * sometimes it is required to store extra data with each array item, a junction
 * class is for this reason. The name of a junction* class should be a combination
 * of the two model names e.g. "Model1_Model2" where Model1 or Model2 or both 
 * contain an array data property of the other model.
 *
 * @author Mehdi Torabi <mehdi @ artaengine.com>
 */
interface IJunction {

    /**
     * Reset junction, empty data and set things like the object is first initialized.
     *
     * @return IJunction for method chaining
     */
    public function reset();

    /**
     * A dictionary of junction data properties and their value.
     *
     * @return array {property-name:value,}
     */
    public function values();

    /**
     * A list of junction data properties.
     *
     * @return array [property-name,]
     */
    public function properties();

    /**
     * A dictionary of all junction data properties and their definitions.
     *
     * @return array {property-name:{property-definition},}
     */
    public function cols();
}
