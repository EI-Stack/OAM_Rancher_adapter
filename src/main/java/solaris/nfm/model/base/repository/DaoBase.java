package solaris.nfm.model.base.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import solaris.nfm.model.base.domain.EntityBase;

@NoRepositoryBean
// public interface DaoBase<T extends EntityBase, ID extends Serializable> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T>
// 從 Spring Data 2.x, QueryDslPredicateExecutor --> QuerydslPredicateExecutor  (dsl 的 d 從大寫變成小寫)
public interface DaoBase<T extends EntityBase, ID extends Serializable> extends JpaRepository<T, ID>, QuerydslPredicateExecutor<T>
{}
