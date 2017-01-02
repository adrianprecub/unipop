package org.unipop.jdbc;

import com.google.common.collect.Sets;
import org.jooq.Condition;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.json.JSONObject;
import org.unipop.common.util.PredicatesTranslator;
import org.unipop.jdbc.controller.simple.RowController;
import org.unipop.jdbc.controller.simple.RowOptimizedController;
import org.unipop.jdbc.schemas.RowEdgeSchema;
import org.unipop.jdbc.schemas.RowVertexSchema;
import org.unipop.jdbc.schemas.jdbc.JdbcSchema;
import org.unipop.jdbc.utils.ContextManager;
import org.unipop.jdbc.utils.JdbcPredicatesTranslator;
import org.unipop.jdbc.utils.TimingExecuterListener;
import org.unipop.query.controller.SourceProvider;
import org.unipop.query.controller.UniQueryController;
import org.unipop.structure.UniGraph;

import java.sql.Connection;
import java.util.Set;
import java.util.function.Supplier;

import static org.unipop.util.ConversionUtils.getList;

/**
 * @author Gur Ronen
 * @since 21/6/2016
 */
public class JdbcSourceProvider implements SourceProvider {
    private final Supplier<PredicatesTranslator<Condition>> predicatesTranslatorSupplier;
    private UniGraph graph;
    private ContextManager contextManager;

    public JdbcSourceProvider() {
        this(JdbcPredicatesTranslator::new);
    }

    public JdbcSourceProvider(Supplier<PredicatesTranslator<Condition>> predicatesTranslatorSupplier) {
        this.predicatesTranslatorSupplier = predicatesTranslatorSupplier;
    }

    @Override
    public Set<UniQueryController> init(UniGraph graph, JSONObject configuration) throws Exception {

        boolean optimized = false;
        SQLDialect dialect = SQLDialect.valueOf(configuration.getString("sqlDialect"));
        if (dialect.equals(SQLDialect.POSTGRES)) optimized = true;
        if (configuration.optBoolean("optimized", false)) optimized = true;
        this.contextManager = new ContextManager(configuration);

        this.graph = graph;

        Set<JdbcSchema> schemas = Sets.newHashSet();

        getList(configuration, "vertices").forEach(vertexJson -> schemas.add(createVertexSchema(vertexJson)));
        getList(configuration, "edges").forEach(edgeJson -> schemas.add(createEdgeSchema(edgeJson)));

        return createControllers(schemas, optimized);
    }

    @Override
    public void close() {
        this.contextManager.close();
    }

    public RowVertexSchema createVertexSchema(JSONObject vertexJson) {
        return new RowVertexSchema(vertexJson, this.graph);
    }

    public RowEdgeSchema createEdgeSchema(JSONObject edgeJson) {
        return new RowEdgeSchema(edgeJson, this.graph);
    }

    public Set<UniQueryController> createControllers(Set<JdbcSchema> schemas, boolean optimized) {
        RowController rowController;
        if (!optimized)
         rowController = new RowController(this.graph, this.contextManager, schemas, this.predicatesTranslatorSupplier.get());
        else
            rowController = new RowOptimizedController(this.graph, this.contextManager, schemas, this.predicatesTranslatorSupplier.get());
        return Sets.newHashSet(rowController);
    }

    @Override
    public String toString() {
        return "JdbcSourceProvider{" +
                "contextManager=" + contextManager +
                ", predicatesTranslatorSupplier=" + predicatesTranslatorSupplier +
                ", graph=" + graph +
                '}';
    }
}
