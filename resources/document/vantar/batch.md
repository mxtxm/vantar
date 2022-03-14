# Vantar batch data action #
## batch params ##
    JSON
    {
        "lang": "fa",
        "insert": [{OBJECT},...],
        "update": [{OBJECT},...],
        "delete": [1,3,34,...]
    }
* {OBJECT} means key value pairs depending on the object. (see the fields of object of each service)
* lang: (String) (default=SYSTEM-DEFINED-LANG) "fa" or "en" or ... 
* insert: (list of objects) to insert
* update: (list of objects) to update
* delete: (list of (long) ids) to delete
## batch output ##
    String: success message








