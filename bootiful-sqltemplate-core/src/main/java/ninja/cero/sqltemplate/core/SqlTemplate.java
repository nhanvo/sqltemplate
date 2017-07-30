package ninja.cero.sqltemplate.core;

import ninja.cero.sqltemplate.core.mapper.MapperBuilder;
import ninja.cero.sqltemplate.core.parameter.BeanParameter;
import ninja.cero.sqltemplate.core.parameter.MapParameter;
import ninja.cero.sqltemplate.core.parameter.ParamBuilder;
import ninja.cero.sqltemplate.core.stream.StreamQuery;
import ninja.cero.sqltemplate.core.stream.StreamResultSetExtractor;
import ninja.cero.sqltemplate.core.template.TemplateEngine;
import ninja.cero.sqltemplate.core.util.TypeUtils;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class SqlTemplate {
    public static final Object[] EMPTY_ARGS = new Object[0];
    protected final JdbcTemplate jdbcTemplate;

    protected final NamedParameterJdbcTemplate namedJdbcTemplate;

    protected TemplateEngine templateEngine;

    protected ParamBuilder paramBuilder;

    protected MapperBuilder mapperBuilder;

    public SqlTemplate(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate) {
        this(jdbcTemplate, namedJdbcTemplate, TemplateEngine.TEXT_FILE, new ParamBuilder(), new MapperBuilder());
    }

    /**
     * Deprecated.
     * Use {@link FreeMarkerSqlTemplate}, {@link PlainTextSqlTemplate} or original class extends {@link SqlTemplate}.
     *
     * @param jdbcTemplate      The JdbcTemplate to use
     * @param namedJdbcTemplate The NamedParameterJdbcTemplate to use
     * @param templateEngine    The tepmlate engine to use
     */
    @Deprecated
    public SqlTemplate(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate, TemplateEngine templateEngine) {
        this(jdbcTemplate, namedJdbcTemplate, templateEngine, new ParamBuilder(), new MapperBuilder());
    }

    public SqlTemplate(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate, ZoneId zoneId) {
        this(jdbcTemplate, namedJdbcTemplate, TemplateEngine.TEXT_FILE, new ParamBuilder(zoneId), new MapperBuilder(zoneId));
    }

    /**
     * Deprecated.
     * Use {@link FreeMarkerSqlTemplate}, {@link PlainTextSqlTemplate} or original class extends {@link SqlTemplate}.
     *
     * @param jdbcTemplate      The JdbcTemplate to use
     * @param namedJdbcTemplate The NamedParameterJdbcTemplate to use
     * @param templateEngine    The tepmlate engine to use
     * @param zoneId            The zoneId for zone aware date type such as ZonedDateTime, OffsetDateTime
     */
    @Deprecated
    public SqlTemplate(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate, TemplateEngine templateEngine, ZoneId zoneId) {
        this(jdbcTemplate, namedJdbcTemplate, templateEngine, new ParamBuilder(zoneId), new MapperBuilder(zoneId));
    }

    public SqlTemplate(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate, TemplateEngine templateEngine, ParamBuilder paramBuilder, MapperBuilder mapperBuilder) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.templateEngine = templateEngine;
        this.paramBuilder = paramBuilder;
        this.mapperBuilder = mapperBuilder;
    }

    public <T> T forObject(String fileName, Class<T> clazz) {
        List<T> list = forList(fileName, clazz);
        return DataAccessUtils.singleResult(list);
    }

    public <T> T forObject(String fileName, Class<T> clazz, Object... args) {
        List<T> list = forList(fileName, clazz, args);
        return DataAccessUtils.singleResult(list);
    }

    public <T> T forObject(String fileName, Class<T> clazz, Map<String, Object> params) {
        List<T> list = forList(fileName, clazz, params);
        return DataAccessUtils.singleResult(list);
    }

    public <T> T forObject(String fileName, Class<T> clazz, Object entity) {
        List<T> list = forList(fileName, clazz, entity);
        return DataAccessUtils.singleResult(list);
    }

    public <T> List<T> forList(String fileName, Class<T> clazz) {
        String sql = getTemplate(fileName, EMPTY_ARGS);
        return jdbcTemplate.query(sql, mapperBuilder.mapper(clazz));
    }

    public <T> List<T> forList(String fileName, Class<T> clazz, Object... args) {
        String sql = getTemplate(fileName, args);
        return jdbcTemplate.query(sql, paramBuilder.byArgs(args), mapperBuilder.mapper(clazz));
    }

    public <T> List<T> forList(String fileName, Class<T> clazz, Map<String, Object> params) {
        String sql = getTemplate(fileName, params);
        return namedJdbcTemplate.query(sql, paramBuilder.byMap(params), mapperBuilder.mapper(clazz));
    }

    public <T> List<T> forList(String fileName, Class<T> clazz, Object entity) {
        String sql = getTemplate(fileName, entity);

        if (TypeUtils.isSimpleValueType(entity.getClass())) {
            return jdbcTemplate.query(sql, paramBuilder.byArgs(entity), mapperBuilder.mapper(clazz));
        }

        return namedJdbcTemplate.query(sql, paramBuilder.byBean(entity), mapperBuilder.mapper(clazz));
    }

    public <T> StreamQuery<T> forStream(String fileName, Class<T> clazz) {
        return new StreamQuery<T>() {
            @Override public <U> U in(Function<? super Stream<T>, U> handleStream) {
                String sql = getTemplate(fileName, EMPTY_ARGS);
                RowMapper<T> mapper = mapperBuilder.mapper(clazz);
                SQLExceptionTranslator excTranslator = jdbcTemplate.getExceptionTranslator();
                ResultSetExtractor<U> extractor = new StreamResultSetExtractor(sql, mapper, handleStream, excTranslator);
                return jdbcTemplate.query(sql, extractor);
            }
        };
    }

    public <T> StreamQuery<T> forStream(String fileName, Class<T> clazz, Object... args) {
        return new StreamQuery<T>() {
            @Override public <U> U in(Function<? super Stream<T>, U> handleStream) {
                String sql = getTemplate(fileName, args);
                RowMapper<T> mapper = mapperBuilder.mapper(clazz);
                SQLExceptionTranslator excTranslator = jdbcTemplate.getExceptionTranslator();
                ResultSetExtractor<U> extractor = new StreamResultSetExtractor(sql, mapper, handleStream, excTranslator);
                return jdbcTemplate.query(sql, paramBuilder.byArgs(args), extractor);
            }
        };
    }

    public <T> StreamQuery<T> forStream(String fileName, Class<T> clazz, Map<String, Object> params) {
        return new StreamQuery<T>() {
            @Override public <U> U in(Function<? super Stream<T>, U> handleStream) {
                String sql = getTemplate(fileName, params);
                RowMapper<T> mapper = mapperBuilder.mapper(clazz);
                SQLExceptionTranslator excTranslator = jdbcTemplate.getExceptionTranslator();
                ResultSetExtractor<U> extractor = new StreamResultSetExtractor(sql, mapper, handleStream, excTranslator);
                return namedJdbcTemplate.query(sql, paramBuilder.byMap(params), extractor);
            }
        };
    }

    public <T> StreamQuery<T> forStream(String fileName, Class<T> clazz, Object entity) {
        return new StreamQuery<T>() {
            @Override public <U> U in(Function<? super Stream<T>, U> handleStream) {
                String sql = getTemplate(fileName, entity);
                RowMapper<T> mapper = mapperBuilder.mapper(clazz);
                SQLExceptionTranslator excTranslator = jdbcTemplate.getExceptionTranslator();
                ResultSetExtractor<U> extractor = new StreamResultSetExtractor(sql, mapper, handleStream, excTranslator);

                if (TypeUtils.isSimpleValueType(entity.getClass())) {
                    return jdbcTemplate.query(sql, paramBuilder.byArgs(entity), extractor);
                }

                return namedJdbcTemplate.query(sql, paramBuilder.byBean(entity), extractor);
            }
        };
    }

    public Map<String, Object> forMap(String fileName) {
        List<Map<String, Object>> list = forList(fileName);
        return DataAccessUtils.singleResult(list);
    }

    public Map<String, Object> forMap(String fileName, Object... args) {
        List<Map<String, Object>> list = forList(fileName, args);
        return DataAccessUtils.singleResult(list);
    }

    public Map<String, Object> forMap(String fileName, Map<String, Object> params) {
        List<Map<String, Object>> list = forList(fileName, params);
        return DataAccessUtils.singleResult(list);
    }

    public Map<String, Object> forMap(String fileName, Object entity) {
        List<Map<String, Object>> list = forList(fileName, entity);
        return DataAccessUtils.singleResult(list);
    }

    public List<Map<String, Object>> forList(String fileName) {
        String sql = getTemplate(fileName, EMPTY_ARGS);
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> forList(String fileName, Object... args) {
        String sql = getTemplate(fileName, args);
        return jdbcTemplate.query(sql, paramBuilder.byArgs(args), new ColumnMapRowMapper());
    }

    public List<Map<String, Object>> forList(String fileName, Map<String, Object> params) {
        String sql = getTemplate(fileName, params);
        return namedJdbcTemplate.queryForList(sql, paramBuilder.byMap(params));
    }

    public List<Map<String, Object>> forList(String fileName, Object entity) {
        String sql = getTemplate(fileName, entity);

        if (TypeUtils.isSimpleValueType(entity.getClass())) {
            return jdbcTemplate.queryForList(sql, entity);
        }

        return namedJdbcTemplate.queryForList(sql, paramBuilder.byBean(entity));
    }

    /**
     * Makes a stream query for the column maps.
     *
     * @param fileName SQL specifier
     * @return a stream query for the column maps
     * @throws DataAccessException if there is any problem
     */
    public StreamQuery<Map<String, Object>> forStream(String fileName) {
        return new StreamQuery<Map<String, Object>>() {
            @Override public <U> U in(Function<? super Stream<Map<String, Object>>, U> handleStream) {
                String sql = getTemplate(fileName, EMPTY_ARGS);
                RowMapper<Map<String, Object>> mapper = new ColumnMapRowMapper();
                SQLExceptionTranslator excTranslator = jdbcTemplate.getExceptionTranslator();
                ResultSetExtractor<U> extractor = new StreamResultSetExtractor(sql, mapper, handleStream, excTranslator);
                return jdbcTemplate.query(sql, extractor);
            }
        };
    }

    /**
     * Makes a stream query for the column maps,
     * using {@code params} as the named parameters.
     *
     * @param fileName SQL template specifier
     * @param params the named parameters
     * @return a stream query for the column maps
     * @throws DataAccessException if there is any problem
     */
    public StreamQuery<Map<String, Object>> forStream(String fileName, Map<String, Object> params) {
        return new StreamQuery<Map<String, Object>>() {
            @Override public <U> U in(Function<? super Stream<Map<String, Object>>, U> handleStream) {
                String sql = getTemplate(fileName, params);
                RowMapper<Map<String, Object>> mapper = new ColumnMapRowMapper();
                SQLExceptionTranslator excTranslator = jdbcTemplate.getExceptionTranslator();
                ResultSetExtractor<U> extractor = new StreamResultSetExtractor(sql, mapper, handleStream, excTranslator);
                return namedJdbcTemplate.query(sql, paramBuilder.byMap(params), extractor);
            }
        };
    }

    /**
     * Makes a stream query for the column maps,
     * using {@code entity} as the single parameter if it is a simple value;
     * or as the container of the named parameters if it is a bean.
     *
     * @param fileName SQL template specifier
     * @param entity the single parameter or the container of the named parameters
     * @return a stream query for the column maps
     * @throws DataAccessException if there is any problem
     */
    public StreamQuery<Map<String, Object>> forStream(String fileName, Object entity) {
        return new StreamQuery<Map<String, Object>>() {
            @Override public <U> U in(Function<? super Stream<Map<String, Object>>, U> handleStream) {
                String sql = getTemplate(fileName, entity);
                RowMapper<Map<String, Object>> mapper = new ColumnMapRowMapper();
                SQLExceptionTranslator excTranslator = jdbcTemplate.getExceptionTranslator();
                ResultSetExtractor<U> extractor = new StreamResultSetExtractor(sql, mapper, handleStream, excTranslator);

                if (TypeUtils.isSimpleValueType(entity.getClass())) {
                    return jdbcTemplate.query(sql, paramBuilder.byArgs(entity), extractor);
                }

                return namedJdbcTemplate.query(sql, paramBuilder.byBean(entity), extractor);
            }
        };
    }

    /**
     * Makes a stream query for the column maps,
     * using {@code args} as the parameters.
     *
     * @param fileName SQL template specifier
     * @param args the parameters
     * @return a stream query for the column maps
     * @throws DataAccessException if there is any problem
     */
    public StreamQuery<Map<String, Object>> forStream(String fileName, Object... args) {
        return new StreamQuery<Map<String, Object>>() {
            @Override public <U> U in(Function<? super Stream<Map<String, Object>>, U> handleStream) {
                String sql = getTemplate(fileName, args);
                RowMapper<Map<String, Object>> mapper = new ColumnMapRowMapper();
                SQLExceptionTranslator excTranslator = jdbcTemplate.getExceptionTranslator();
                ResultSetExtractor<U> extractor = new StreamResultSetExtractor(sql, mapper, handleStream, excTranslator);
                return jdbcTemplate.query(sql, paramBuilder.byArgs(args), extractor);
            }
        };
    }

    public int update(String fileName, Object... args) {
        String sql = getTemplate(fileName, args);
        return jdbcTemplate.update(sql, paramBuilder.byArgs(args));
    }

    public int update(String fileName, Map<String, Object> params) {
        String sql = getTemplate(fileName, params);
        return namedJdbcTemplate.update(sql, paramBuilder.byMap(params));
    }

    public int update(String fileName, Object entity) {
        String sql = getTemplate(fileName, entity);

        if (TypeUtils.isSimpleValueType(entity.getClass())) {
            return jdbcTemplate.update(sql, paramBuilder.byArgs(entity));
        }

        return namedJdbcTemplate.update(sql, paramBuilder.byBean(entity));
    }

    public int[] batchUpdate(String... fileNames) {
        return jdbcTemplate.batchUpdate(fileNames);
    }

    public int[] batchUpdate(String fileName, Object[][] batchParams) {
        String sql = getTemplate(fileName, EMPTY_ARGS);
        return jdbcTemplate.batchUpdate(sql, paramBuilder.byBatchArgs(batchParams));
    }

    public int[] batchUpdate(String fileName, Map<String, Object>[] batchParams) {
        String sql = getTemplate(fileName, EMPTY_ARGS);

        MapParameter[] params = new MapParameter[batchParams.length];
        for (int i = 0; i < batchParams.length; i++) {
            params[i] = paramBuilder.byMap(batchParams[i]);
        }

        return namedJdbcTemplate.batchUpdate(sql, params);
    }

    public int[] batchUpdate(String fileName, Object[] batchParams) {
        String sql = getTemplate(fileName, EMPTY_ARGS);
        if (batchParams.length == 0) {
            return jdbcTemplate.batchUpdate(sql);
        }

        if (TypeUtils.isSimpleValueType(batchParams[0].getClass())) {
            return jdbcTemplate.batchUpdate(sql, paramBuilder.byBatchArgs(batchParams));
        }

        BeanParameter[] params = new BeanParameter[batchParams.length];
        for (int i = 0; i < batchParams.length; i++) {
            params[i] = paramBuilder.byBean(batchParams[i]);
        }

        return namedJdbcTemplate.batchUpdate(sql, params);
    }

    public MapUpdateBuilder update(String fileName) {
        return new MapUpdateBuilder(fileName);
    }

    public <T> MapQueryBuilderForBean<T> query(String fileName, Class<T> clazz) {
        return new MapQueryBuilderForBean<>(fileName, clazz);
    }

    public MapQueryBuilderForMap query(String fileName) {
        return new MapQueryBuilderForMap(fileName);
    }

    protected String getTemplate(String fileName, Object[] args) {
        try {
            return templateEngine.get(fileName, args);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    protected String getTemplate(String fileName, Object param) {
        try {
            return templateEngine.get(fileName, param);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public class MapQueryBuilderForBean<T> {
        protected Map<String, Object> params = new HashMap<>();
        protected String fileName;
        protected Class<T> clazz;

        public MapQueryBuilderForBean(String fileName, Class<T> clazz) {
            this.fileName = fileName;
            this.clazz = clazz;
        }

        public MapQueryBuilderForBean<T> add(String key, Object value) {
            params.put(key, value);
            return this;
        }

        public T forObject() {
            List<T> list = forList();
            return DataAccessUtils.singleResult(list);
        }

        public List<T> forList() {
            String sql = getTemplate(fileName, params);
            return namedJdbcTemplate.query(sql, paramBuilder.byMap(params), mapperBuilder.mapper(clazz));
        }
    }

    public class MapQueryBuilderForMap {
        protected Map<String, Object> params = new HashMap<>();
        protected String fileName;

        public MapQueryBuilderForMap(String fileName) {
            this.fileName = fileName;
        }

        public MapQueryBuilderForMap add(String key, Object value) {
            params.put(key, value);
            return this;
        }

        public Map<String, Object> forMap() {
            String sql = getTemplate(fileName, params);
            return namedJdbcTemplate.queryForMap(sql, paramBuilder.byMap(params));
        }

        public List<Map<String, Object>> forList() {
            String sql = getTemplate(fileName, params);
            return namedJdbcTemplate.queryForList(sql, paramBuilder.byMap(params));
        }
    }

    public class MapUpdateBuilder {
        protected Map<String, Object> params = new HashMap<>();
        protected String fileName;

        public MapUpdateBuilder(String fileName) {
            this.fileName = fileName;
        }

        public MapUpdateBuilder add(String key, Object value) {
            params.put(key, value);
            return this;
        }

        public int execute() {
            return update(fileName, params);
        }
    }
}
