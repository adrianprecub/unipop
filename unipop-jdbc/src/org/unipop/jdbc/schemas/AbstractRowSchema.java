package org.unipop.jdbc.schemas;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.json.JSONObject;
import org.unipop.jdbc.controller.simple.results.ElementMapper;
import org.unipop.jdbc.schemas.jdbc.JdbcSchema;
import org.unipop.jdbc.utils.JdbcPredicatesTranslator;
import org.unipop.query.predicates.PredicateQuery;
import org.unipop.query.predicates.PredicatesHolder;
import org.unipop.query.search.SearchQuery;
import org.unipop.schema.element.AbstractElementSchema;
import org.unipop.structure.UniGraph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Gur Ronen
 * @since 3/7/2016
 */
public abstract class AbstractRowSchema<E extends Element> extends AbstractElementSchema<E> implements JdbcSchema<E> {
    private final String table;

    public AbstractRowSchema(JSONObject configuration, UniGraph graph) {
        super(configuration, graph);
        this.table = configuration.optString("table");
    }

    @Override
    public String getTable() {
        return this.table;
    }

    @Override
    public Object getId(E element) {
        return element.id();
    }

    @Override
    public List<E> parseResults(Result result, PredicateQuery query) {
        return result.map(new ElementMapper<>(Collections.singleton(this)));
    }

    @Override
    public Select getSearch(SearchQuery<E> query, PredicatesHolder predicates, DSLContext context) {
        Condition conditions = new JdbcPredicatesTranslator().translate(predicates);
        int finalLimit = query.getLimit() < 0 ? Integer.MAX_VALUE : query.getLimit();

        return createSqlQuery(query.getPropertyKeys(), context)
                .where(conditions)
                .limit(finalLimit);

    }


    private <E extends Element> SelectJoinStep<Record> createSqlQuery(Set<String> columnsToRetrieve, DSLContext context) {
        if (columnsToRetrieve == null) {
            return context.select().from(this.getTable());

        }

        Set<String> props = this.toFields(columnsToRetrieve);

        return context
                .select(props.stream().map(DSL::field).collect(Collectors.toList()))
                .from(this.getTable());
    }

    @Override
    public String toString() {
        return "AbstractRowSchema{" +
                "table='" + table + '\'' +
                "} " + super.toString();
    }
}
