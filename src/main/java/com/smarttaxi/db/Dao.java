package com.smarttaxi.db;

import java.util.List;
import java.util.Optional;

import com.smarttaxi.exception.DataAccessException;

/** Basic create, read, update and delete operations for one table. */
public interface Dao<T, ID> {

    void insert(T item) throws DataAccessException;

    Optional<T> findById(ID id) throws DataAccessException;

    List<T> findAll() throws DataAccessException;

    void update(T item) throws DataAccessException;

    void delete(ID id) throws DataAccessException;
}
