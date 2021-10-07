# Vantar search #

## search params ##
    JSON
    {
        "lang": "fa",
        "page": 1,
        "length": 10,
        "offset": 0,
        "limit": 10,
        "pagination": false,
        "total": 1000,
        "sort": ["name:asc", "id:desc"],
        "condition": {
            "operator": "AND",
            "items": [
                {"type": "FULL_SEARCH", "value": "resistor"},        
                {"col": "name", "type": "LIKE", "value": "resistor"},  
                {"col": "id", "type": "GREATER_THAN_EQUAL", "value": 1},
                {"col": "id", "type": "IN", "values": [1,7,88]},
                {"col": "publishDateTime", "type": "BETWEEN", "values": ["1390-1-2","1396-1-2"]},
                {"type": "CONDITION", "condition": {recursive, another condition with same format}}
            ]
        }
    }
### parameters ###
* lang (String, default=SYSTEM-DEFINED-LANG): "fa" or "en" or ... 
* sort (list of strings): each item consists of field name:asc / desc
* condition:
    * operator: (String case insensitive of "AND" "OR" "NOT" "NOR")
    * type (String case insensitive): condition type
        * "EQUAL"
        * "NOT_EQUAL"
        * "LIKE"
        * "NOT_LIKE"
        * "IN"
        * "NOT_IN"
        * "FULL_SEARCH"
        * "BETWEEN" 
        * "NOT_BETWEEN"
        * "LESS_THAN"
        * "GREATER_THAN"
        * "LESS_THAN_EQUAL"
        * "GREATER_THAN_EQUAL"
        * "IS_NULL"
        * "IS_NOT_NULL"
        * "IS_EMPTY"
        * "IS_NOT_EMPTY"
        * "CONDITION" 
    * col (String): column name to execute condition against. type "CONDITION" and "FULL_SEARCH" do not have col.
        * "FULL_SEARCH" runs a generic search against all text columns, suitable for generic searchs.
        * "CONDITION" to add complex mixed condition with the same structure (recursive) as described now.
    * value (Object): value to search for, use this single value conditions.
    * values (List): values to search for, use this for two or multi value conditions ("IN" and "BETWEEN")
    * condition: 
### limiting result and pagination parameters ###
page model of data: dividing data into multiple pages with a fixed amount of records, for example if you want to fetch 10
records each time from the server page 1 is the first 10 records, page 2 is the second 10 records ....

* page: (int >= 1) page number
* length: (int, default=10) max number of records in a page

offset model of data: to fetch a fixed number of records from and offset, offset is the record number starting from 0, for
example if you want to fetch 10 records starting from record number 0(first) or record number 9...

* offset: (int) >=0 record number to start from
* limit: (int, default=10) number of records to fetch

pagination: the above limiting options can be used to limit results or fetch data for a box with a "get more data" option...
to have pagination enable pagination: after the first fetch total number of records will be passed. getting total is expensive
when you get it always add it to the next requests parameters:

* pagination: (boolean) (default=false) turn pagination on and off
* total: (int) total number of records. always set this if available, set to 0 for first call, set to the total value which
  is returned with the first search result for subsequent page calls. number of pages = total/length
### notes ###
* all parameters are optional, the params can be used for single usages like limiting or sorting data ro for simple generic
  search or complex multi condition searches 
* commands are case insensitive.
* only use (page and length) or (offset and limit) do not use together 
### example ###
all records with pagination (first call):

    {
        "pagination": true,
        "lang": "fa",
        "page": 1,
        "length": 10,
        "total": 0,
        "sort": ["name:asc", "id:desc"]
    }

(next call for 2nd page, 5003 is the total number of records from first result):

    {
        "pagination": true,
        "lang": "fa",
        "page": 2,
        "length": 10,
        "total": 5003,
        "sort": ["name:asc", "id:desc"]
    }

### example ###
search by a keyword get first 10 records without pagination:

    {
        "lang": "fa",
        "page": 1,
        "length": 10,
        "total": 1000,
        "sort": ["name:asc"],
        "condition": {
            "fields": {
                {"type": "FULL_SEARCH", "value": "resistor"}        
            }
        }
    }

same as above:

    {
        "lang": "fa",
        "limit": 10,
        "offset": 0,
        "sort": ["name:asc"],
        "condition": {
            "fields": [
                {"type": "FULL_SEARCH", "value": "resistor"}        
            ]
        }
    }

(after "show more records"):

    {
        "lang": "fa",
        "limit": 10,
        "offset": 10,
        "sort": ["name:asc"],
        "condition": {
            "fields": [
                {"type": "FULL_SEARCH", "value": "resistor"}        
            ]
        }
    }

(until status 204 NoContentException Exception)

## search output ##
pagination=false

    JSON
    [{OBJECT},]

pagination=true

    JSON
    {
        "data": [{OBJECT},],
        "page": 2,
        "length": 10,
        "total": 103023,
    }
* {OBJECT} means key value pairs depending on the object being searched. (see the fields of object of each service)
* page: the page number of the fetched data
* length: max records per page (to let calculate number of pages)
* total: total number of search results (must pass this value with next requests)

## examples ##
    Map<String, Object> specs =
    {
        "name": "mehdi",                     // STRING  ----(address in search)----> "specs.name"
        "age": 41,                           // NUMBER  ----(address in search)----> "specs.age"
        "enabled": false,                    // BOOLEAN ----(address in search)----> "specs.enabled"
        "amount": 2.1,                       // NUMBER  ----(address in search)----> "amount"
        "volt": {"min": 110, "max": 240},    // RANGE   ----(address in search)----> "specs.volt.min" or "specs.volt.max"
        "steps": [2,4,6,8]                   // LIST    ----(address in search)----> "specs.steps"
        "kv": {"a":"aa", "b":"bb", "c":"cc"} // MAP     ----(address in search)----> "specs.kv.a" 
    }

## advanced search params (experimental, not intended for UI) ##
    JSON
    {
        "dto": "User", 
        "dtoResult": "User", 
        "lang": "fa",
        "page": 1,
        "length": 10,
        "offset": 0,
        "limit": 10,
        "pagination": false,
        "total": 1000,
        "sort": ["name:asc", "id:desc"],
        "condition": {
            "operator": "AND",
            "items": [
                {"type": "FULL_SEARCH", "value": "resistor"},        
                {"col": "name", "type": "LIKE", "value": "resistor"},  
                {"col": "id", "type": "GREATER_THAN_EQUAL", "value": 1},
                {"col": "id", "type": "IN", "values": [1,7,88]},
                {"col": "publishDateTime", "type": "BETWEEN", "values": ["1390-1-2","1396-1-2"]},
                {"type": "CONDITION", "condition": {recursive, another condition with same format}}
            ]
        },
        "conditionGroup": {

        },
        "columns": ["name", "User.name", ""],
        "joins": [
            {
                "joinType": "INNER JOIN",
                "dtoRight": "Seller"
            },
            {
                "joinType": "INNER JOIN",
                "dtoRight": "Seller",
                "keyLeft": "userId",
                "keyRight": "id"
            },
            {
                "joinType": "INNER JOIN",
                "dtoRight": "Seller",
                "keyLeft": "userId",
                "keyRight": "id",
                "as": "seller1"
            },
            {
                "joinType": "INNER JOIN",
                "dtoLeft": "Seller",
                "dtoRight": "Shop"
            }
        ],
        "group": [
            "column": "qty"
        ]
        "group": [
            "column": "qty",
            "columnAs": "total"
        ]
        "group": [
            "column": "qty",
            "groupType": "COUNT"
        ]
        "group": [
            "column": "qty",
            "columnAs": "total",
            "groupType": "COUNT"
        ]
    }
### parameters ###
* dto (String): dto to query data on (repo) 
* dtoResult (String): dto to put result in (if not provided then dto is used) 
* conditionGroup: condition for SQL HAVING - the same as "condition"
* columns (list of String): columns to fetch
* joins: for SQL
    * dtoLeft (String): left Dto name to join against default is query dto
    * dtoRight (String): right Dto name to join against
    * keyLeft (String): dto column to join on, usually dto fk
    * keyRight (String): dtoRight column to join on, usually dtoRight pk
    * as (String): alias for dtoRight
    * joinType (String):
        * "JOIN"
        * "LEFT JOIN"
        * "FULL JOIN"
        * "," (self join)
        * "CROSS JOIN"
        * "NATURAL JOIN"
* group:
    * column (String): column to aggregate
    * columnAs (String): new column name
    * groupType (String):
        * "SUM"
        * "MAP"
        * "AVG"
        * "COUNT"
        * "MIN"
        * "MAX"
