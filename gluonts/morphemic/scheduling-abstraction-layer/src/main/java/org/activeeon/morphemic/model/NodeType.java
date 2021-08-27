package org.activeeon.morphemic.model;

import org.eclipse.emf.common.util.Enumerator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Node Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 */
public enum NodeType implements Enumerator {
    /**
     * The '<em><b>IAAS</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #IAAS_VALUE
     * @generated
     * @ordered
     */
    IAAS(0, "IAAS", "IAAS"),

    /**
     * The '<em><b>PAAS</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #PAAS_VALUE
     * @generated
     * @ordered
     */
    PAAS(1, "PAAS", "PAAS"),

    /**
     * The '<em><b>FAAS</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #FAAS_VALUE
     * @generated
     * @ordered
     */
    FAAS(2, "FAAS", "FAAS"),

    /**
     * The '<em><b>BYON</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #BYON_VALUE
     * @generated
     * @ordered
     */
    BYON(3, "BYON", "BYON"), /**
     * The '<em><b>SIMULATION</b></em>' literal object.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #SIMULATION_VALUE
     * @generated
     * @ordered
     */
    SIMULATION(4, "SIMULATION", "SIMULATION");

    /**
     * The '<em><b>IAAS</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>IAAS</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @see #IAAS
     * @model
     * @generated
     * @ordered
     */
    public static final int IAAS_VALUE = 0;

    /**
     * The '<em><b>PAAS</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>PAAS</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @see #PAAS
     * @model
     * @generated
     * @ordered
     */
    public static final int PAAS_VALUE = 1;

    /**
     * The '<em><b>FAAS</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>FAAS</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @see #FAAS
     * @model
     * @generated
     * @ordered
     */
    public static final int FAAS_VALUE = 2;

    /**
     * The '<em><b>BYON</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>BYON</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @see #BYON
     * @model
     * @generated
     * @ordered
     */
    public static final int BYON_VALUE = 3;

    /**
     * The '<em><b>SIMULATION</b></em>' literal value.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of '<em><b>SIMULATION</b></em>' literal object isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @see #SIMULATION
     * @model
     * @generated
     * @ordered
     */
    public static final int SIMULATION_VALUE = 4;

    /**
     * An array of all the '<em><b>Node Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    private static final NodeType[] VALUES_ARRAY =
            new NodeType[] {
                    IAAS,
                    PAAS,
                    FAAS,
                    BYON,
                    SIMULATION,
            };

    /**
     * A public read-only list of all the '<em><b>Node Type</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public static final List<NodeType> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Node Type</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param literal the literal.
     * @return the matching enumerator or <code>null</code>.
     * @generated
     */
    public static NodeType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            NodeType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Node Type</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param name the name.
     * @return the matching enumerator or <code>null</code>.
     * @generated
     */
    public static NodeType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            NodeType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Node Type</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the integer value.
     * @return the matching enumerator or <code>null</code>.
     * @generated
     */
    public static NodeType get(int value) {
        switch (value) {
            case IAAS_VALUE: return IAAS;
            case PAAS_VALUE: return PAAS;
            case FAAS_VALUE: return FAAS;
            case BYON_VALUE: return BYON;
            case SIMULATION_VALUE: return SIMULATION;
        }
        return null;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    private final int value;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    private final String name;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    private final String literal;

    /**
     * Only this class can construct instances.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    private NodeType(int value, String name, String literal) {
        this.value = value;
        this.name = name;
        this.literal = literal;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public int getValue() {
        return value;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public String getName() {
        return name;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public String getLiteral() {
        return literal;
    }

    /**
     * Returns the literal value of the enumerator, which is its string representation.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @Override
    public String toString() {
        return literal;
    }

} //NodeType