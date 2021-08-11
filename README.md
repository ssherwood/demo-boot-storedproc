# Demo Spring Boot Project (Postgres + escapeSyntaxCallMode)

This is a small demo project using both Functions and Stored Procedures in Postgres. Specifically,
this project demonstrates some issues with using the `escapeSyntaxCallMode` that was added to the
42.2.16+ Postgres driver.

The implication of using this feature was that by using the option `callIfNoReturn`, it would allow
an existing project to be able to invoke either a Function or Procedure using the same semantics
from the client (in the Spring Boot , and it would properly interpret the syntax based on if it was a function or not.

See more info here:

- https://github.com/davecramer/pgjdbc/commit/77206015f9d46495505c1098faa7cabbee044284
- https://jdbc.postgresql.org/documentation/publicapi/org/postgresql/jdbc/EscapeSyntaxCallMode.html
- https://jdbc.postgresql.org/documentation/head/callproc.html
- https://stackoverflow.com/questions/65696904/postgresql-11-stored-procedure-call-error-to-call-a-procedure-use-call-java

In this project, you can see that there is a very basic function and procedure being created that are
invoked via the Spring Data `@Repository` layer.  Using the `callIfNoReturn` flag, it successfully invokes
the Procedure, but fails to interpret the function call (which clearly has a return).

## Setup

Apply these two sql blocks to your default Postgres database (this project assumes localhost and the
default port):

```sql
CREATE OR REPLACE FUNCTION add_func(arg1 int, arg2 int) RETURNS int AS $$
BEGIN
    return arg1 + arg2;
END;
$$ LANGUAGE 'plpgsql'; 
```

```sql
CREATE OR REPLACE PROCEDURE increment_proc(INOUT arg int) AS $$
BEGIN
    arg = arg + 1;
END;
$$ LANGUAGE 'plpgsql';
```

Run the application as normal and invoke the 2 http endpoints:

```shell
curl -i "http://localhost:8080/increment-proc?arg=9"
```

This call to the Stored Procedure works as expected.  So by the configuration setting, the driver
determined that it should use the CALL syntax instead of the default.

However, when invoking the Function:

```shell
$ curl -i "http://localhost:8080/add-func?arg1=5&arg2=16"
```

This second call fails with an error:

```
org.postgresql.util.PSQLException: ERROR: add(integer, integer) is not a procedure
Hint: To call a function, use SELECT.
```

Since `add` is a function with a return, the expectation is that the setting `callIfNoReturn` would
default to using SELECT.

Alternately, switching the JDBC connection setting to "proc" or "func" and running the tests again does work for the
respective operation (meaning the function call works when in func mode and the procedure works in proc mode).

The interpretation of this behavior seems to indicate that the function, as defined, does not get seen as having a
return type for some reason by the driver, and it does not escape the call properly.