//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.02.26 at 11:00:23 AM CET 
//


package edu.kit.joana.ui.ifc.wala.rifl.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "assign")
public class Assign {

    @XmlAttribute(name = "category", required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String category;
    @XmlAttribute(name = "securitydomain", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String securitydomain;

    /**
     * Gets the value of the category property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the value of the category property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCategory(String value) {
        this.category = value;
    }

    /**
     * Gets the value of the securitydomain property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSecuritydomain() {
        return securitydomain;
    }

    /**
     * Sets the value of the securitydomain property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSecuritydomain(String value) {
        this.securitydomain = value;
    }

}
