# Simple example to explore pure RESTful [ActiveWeb](http://javalite.io/activeweb) possibilities

Related [Blog Article](http://www.productiveedge.com/blog/index.php/exploring-activeweb-pure-restful-possibilities)

Follow these simple steps to run this app:

* Create two empty schemas in MySQL DB:
    * activerest_development
    * activerest_test

* Modify JDBC connection parameters in:
    * class app.config.DbConfig
    * pom.xml

* Run the application

```
    mvn jetty:run
```

* The api can be explored using this [Chrome Postman - Rest Client](https://chrome.google.com/webstore/detail/postman-rest-client/fdmmgilgnpjigdojojpjoooidkmcomcm) example [collection](https://www.getpostman.com/collections/acff504b766cde75d1b5), or through basic HTTP calls (e.g. curl)

* API urls:
    * http://localhost:8080/users
    	* http://localhost:8080/users/new_form
    	* http://localhost:8080/users/{user_id}/edit_form
    * http://localhost:8080/users/{user_id}/tasks
    
  

 
