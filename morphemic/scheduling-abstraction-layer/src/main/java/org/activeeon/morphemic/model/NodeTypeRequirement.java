package org.activeeon.morphemic.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Subtype of Requirement
 */
public class NodeTypeRequirement extends Requirement  {
    @JsonProperty("nodeType")
    private NodeType nodeType;

    /**
     * Get nodeType
     * @return nodeType
     **/
    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NodeTypeRequirement nodeTypeRequirement = (NodeTypeRequirement) o;
        return Objects.equals(this.nodeType, nodeTypeRequirement.nodeType) &&
                super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeType, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class NodeTypeRequirement {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    nodeType: ").append(toIndentedString(nodeType)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}