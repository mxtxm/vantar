# Rules #

## URL paths ##
* first path for all UI webservices start with **/ui/**
* second path is the context for example if webservice is about product it will be **product/**
* third path is the action for example **update/**
* if context or action has more than one syllables then they are separated by **/** for example **/ui/product/item/insert**
* if action fetches data:
    * one record, then context is signal for example **/ui/product/get**
    * many records, then context is plural for example **/ui/product/items/get** or **/ui/products/search**
* if last path is **/keyval** output is a JSON as {key:value,...} for example {1:"Tehran", 2:"Shiraz", 3:"Kerman"}

## objects ##
* when something like **{ObjectName}** is seen it means an object of type ObjectName, find the object in the **object.md**
  document to find out the data fields.

## web services ##


### method ###
* method dosn't matter POST or GET, better to user GET for webservices that do not have params 
* POST JSON means input must be posted as JSON

## headers ##
* any header marked as **bold** is required, other headers are optional
* if web service requires authentication: **String X-Auth-Token: auth token**
* for almost all web services set lang header to user selected lang **String X-Lang: (default=SYSTEM-DEFINED-LANG) "fa" or "en" or ...**
  the returned values and messages will be based on this language

## params ##
* based on **method** input params are send as POST/GET params or JSON
* depending on the context object input params are mostly the same as object properties
* any param marked as **bold** is required, other params are optional

## output ##
if webservice response is properties of an object then its shown as below, find the object in **objects.md** document 


        JSON
        {FeatureSubscribe} 


if webservice response is a list of objects then its shown as below, find the object in **objects.md** document


        JSON
        [{FeatureSubscribe}] 


if webservice returns search results then output is like below, see **search.md**


        JSON
        {
            "pagination": true,
            "lang": "fa",
            "page": 1,
            "length": 10,
            "total": 0,
            "sort": ["name:asc", "id:desc"]
        }

or

        JSON
        {
            "pagination": true,
            "lang": "fa",
            "page": 2,
            "length": 10,
            "total": 5003,
            "sort": ["name:asc", "id:desc"]
        }


if webservice is a write action the output is usually as below:


        JSON
        {
            "code": 200,
            "message": "success message",
            "successful": true
            "value": "created token only for local test"
        }


### exceptions ###
must check the response status code

* 200 when webservice was successful
* 204 when webservice was successful but no content was found to return
* 500 ServerError, if backend failed
* 400 InvalidInputParams, if there are errors in params used to call the service
* 401/403 AuthError/AuthPermissionError, user is not signed in, or auth token was not set in tehe headers or auth token is invalid
  or user has no access