package io.undertree.demo.storedproc.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.stereotype.Repository;

@Repository
public interface SampleDataRepository extends JpaRepository<DummyEntity, Integer> {

    @Procedure(value = "add_func")
        //@Query(value = "SELECT add_func(:arg1, :arg2);", nativeQuery = true)
    Integer addFunc(Integer arg1, Integer arg2);

    @Procedure(value = "increment_proc")
        //@Query(value = "CALL increment_proc(:arg);", nativeQuery = true)
    Integer incrementProc(Integer arg);
}
