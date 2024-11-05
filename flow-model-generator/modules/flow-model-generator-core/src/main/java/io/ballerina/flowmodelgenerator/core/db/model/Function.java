package io.ballerina.flowmodelgenerator.core.db.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.ballerinalang.diagramutil.connector.models.connector.Type;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Function")
public class Function {

    public enum Kind {
        FUNCTION, CONNECTOR, REMOTE, RESOURCE

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "function_id")
    private Long functionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind")
    private Kind kind;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "resource_path")
    private String resourcePath;

    @Column(name = "return_error")
    private Integer returnError;

    @ManyToOne
    @JoinColumn(name = "package_id")
    private Package pack;

    @Column(name = "return_type", columnDefinition = "TEXT")
    @Convert(converter = TypeConverter.class)
    private Type returnType;

    @OneToMany(mappedBy = "function", cascade = CascadeType.ALL)
    private Set<Parameter> parameters = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "FunctionConnector",
            joinColumns = @JoinColumn(name = "function_id"),
            inverseJoinColumns = @JoinColumn(name = "connector_id")
    )
    private Set<Function> connectors = new HashSet<>();

    // Getters and setters
    public Long getFunctionId() {
        return functionId;
    }

    public void setFunctionId(Long functionId) {
        this.functionId = functionId;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Package getPackage() {
        return pack;
    }

    public void setPackage(Package pack) {
        this.pack = pack;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public Set<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(Set<Parameter> parameters) {
        this.parameters = parameters;
    }

    public Set<Function> getConnectors() {
        return connectors;
    }

    public void setConnectors(Set<Function> connectors) {
        this.connectors = connectors;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public Integer getReturnError() {
        return returnError;
    }

    public void setReturnError(Integer returnError) {
        this.returnError = returnError;
    }
}
