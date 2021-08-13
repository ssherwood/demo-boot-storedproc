# Demo Spring Boot Project
## Postgres using escapeSyntaxCallMode=callIfNoReturn

This is a small demo project using Postgres with the configuration setting `escapeSyntaxCallMode=callIfNoReturn`.
The expected behavior is that the Postgres driver (above 42.2.16) will be able to differentiate calls
to Stored Procedures and Functions without needing to make any coding changes.  In this case, using
Spring Data JPA and leveraging the `@Procedure` annotation.

This information has been derived from these specific sources:

- https://stackoverflow.com/questions/65696904/postgresql-11-stored-procedure-call-error-to-call-a-procedure-use-call-java
- https://github.com/davecramer/pgjdbc/commit/77206015f9d46495505c1098faa7cabbee044284
- https://jdbc.postgresql.org/documentation/publicapi/org/postgresql/jdbc/EscapeSyntaxCallMode.html
- https://jdbc.postgresql.org/documentation/head/callproc.html

However, using the default versions of the Postgres driver derived from Spring Boot / Spring Data, it is
apparent that something isn't working correctly with `callIfNoReturn`.  Specifically that calls to
Functions aren't being detected as a call with a return and thus are not falling back to the default
SELECT behavior.

## Project Setup

First, apply these two SQL blocks to your default Postgres database (this project assumes localhost and
the default port):

https://stackoverflow.com/questions/65696904/postgresql-11-stored-procedure-call-error-to-call-a-procedure-use-call-java
https://www.postgresql.org/docs/12/sql-createfunction.html
https://www.postgresql.org/docs/12/sql-createprocedure.html

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