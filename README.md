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

### Additional Research on PostgreSQL INOUT refcursor

First create a dummy table:

```sql
create table dummy(
    id integer generated by default as identity primary key,
     value varchar);
```

Insert some test data:

```sql
insert into dummy(value) values('Test 1');
insert into dummy(value) values('Test 2');
insert into dummy(value) values('Test 3');
insert into dummy(value) values('Test 4');
insert into dummy(value) values('Test 5');
```

Create a stored procedure:

```sql
CREATE OR REPLACE PROCEDURE fetch_dummy(_dummy INOUT refcursor)
AS $body$
BEGIN
    open _dummy for SELECT * from dummy;
END;
$body$ LANGUAGE 'plpgsql';
```

#### NOTES

- Pretty much everything that I tried in Spring Data/JPA failed in some form or another.
- Most issues seem to revolve around `REFCURSOR` being typically used as a return type from PostgreSQL stored `FUNCTION` types.
- To get an `INOUT` with they type of `REFCURSOR` to work, I needed to get down to the actual JDBC connection and manipulate it directly
- There is a surprisingly limited amount of useful information on this topic:
  - https://www.postgresql.org/docs/11/plpgsql-cursors.html (only shows functions)
  - https://www.sqlines.com/oracle-to-postgresql/return_sys_refcursor (only shows functions)
  - https://jdbc.postgresql.org/documentation/head/callproc.html#callfunc-resultset-refcursor (only functions)


### Research on Hikari

This is just a quick test to force specific errors from the database and see how Hikari handles them.  Hint, it resets
the connection.

Create these stored procedures, they don't do anything but raise exceptions.

```sql
create or replace procedure raise08003()
as $body$
begin
  raise exception using errcode = 'connection_does_not_exist';
end;
$body$ language 'plpgsql';
```

```sql
create or replace procedure raise08006()
as $body$
begin
  raise exception using errcode = 'connection_failure';
end;
$body$ language 'plpgsql';
```
