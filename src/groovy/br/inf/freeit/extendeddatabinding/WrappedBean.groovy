package br.inf.freeit.extendeddatabinding

import org.springframework.beans.*
import org.springframework.beans.NullValueInNestedPathException
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import java.beans.PropertyEditor
import org.springframework.validation.Errors
/**
 * Uses a BeanWrapper to get and set properties as strings, 
 * using registered PropertyEditors to convert values.
 * Methods are also forwarded to the bean instance.
 */
class WrappedBean {
	
	private static List wrapperProperties = ["beanWrapper", "wrappedInstance", "rootWrappedInstance", "errors", "metaClass"]
	private static List primitiveTypes = [byte.class, short.class, int.class, long.class, char.class, boolean.class]
	
	private static boolean isBasicType(Class type) {
		if (primitiveTypes.contains(type)) return true
		String name = type.name
		return name.contains("java.") || name.contains("javax.") || name.contains("groovy.")
	}
	
    def BeanWrapper beanWrapper
    private WrappedBean parent
    private String path
    
    /**
     * Constructs a WrappedBean using the given BeanWrapper 
     */
    WrappedBean(BeanWrapper beanWrapper) {
        this.beanWrapper = beanWrapper
    }
    
    /**
     * Constructs a WrappedBean using a default BeanWrapper and the given wrapped instance 
     */
    WrappedBean(Object wrappedInstance) {
        this(new BeanWrapperImpl(wrappedInstance))
    }
    
    /**
     * Constructs a nested WrappedBean 
     */
    WrappedBean(WrappedBean parent, String path, Object bean) {
        this.parent = parent
        this.path = path
        this.beanWrapper = new BeanWrapperImpl(bean, path, getRootWrappedInstance())
    }
    
    /**
     * Returns the wrapped instance of the root wrapper
     */
    Object getRootWrappedInstance() {
    	if (parent == null) {
    		return getWrappedInstance()
    	}
    	return parent.getRootWrappedInstance()
    }
    
    /**
     * Returns the binding result for the underlying bean
     */
    Errors getErrors() {
    	try {
    		return getWrappedInstance().errors
    	} catch (Exception e) {
    		return null
    	}
    }

    /**
     * Sets the binding result for the underlying bean
     */
    void setErrors(Errors errors) {
    	try {
    		getWrappedInstance().errors = errors
    	} catch (Exception e) {
    		//Ignore
    	}
    }
    
    /**
     * Finds a custom editor in the path
     */
    PropertyEditor findCustomEditor(Class type, String path) {
    	PropertyEditor editor = beanWrapper.findCustomEditor(type, path)
    	if (editor == null && parent != null) {
    		editor = parent.findCustomEditor(type, path ? this.path + "." + path : null)
    	}
    	return editor
    }
    
    /**
     * Sets the current wrapped instance
     */
    void setWrappedInstance(Object wrappedInstance) {
    	beanWrapper.wrappedInstance = wrappedInstance
    }

    /**
     * Sets the current wrapped instance
     */
    Object getWrappedInstance() {
    	return beanWrapper.wrappedInstance
    }
    
    /**
     * Returns the value without formatting it as String
     */
    Object getRawProperty(String name) {
    	if (wrapperProperties.contains(name)) {
    		return this.metaClass.getProperty(this, name);
    	}
    	try {
    		return beanWrapper.getPropertyValue(name)
    	} catch (NullValueInNestedPathException e) {
    		return null
    	}
    }
    
    /**
     * Returns a property value formatted to string using custom registered PropertyEditors.
     * When no editor is registered, returns the value wrapped in a nested BeanWrapper.
     * When a property value is null, null is returned.
     */
    Object getProperty(String name) {
        def value = this.getRawProperty(name)
        if (value == null || wrapperProperties.contains(name)) return value
        Class type = value.class
        PropertyEditor editor = findCustomEditor(type, name)
        if (editor) {
            editor.value = value
            return editor.asText
        } else if (isBasicType(type)) {
        	return value.toString()
        } else {
            return new WrappedBean(this, name, value)
        }
    }
    
    /**
     * Sets a property on the underlying bean.
     * When the value is given as string and the expected type is not a string, the conversion is done using custom registerd PropertyEditors.
     * When the property value is nested AND is quoted, it will try to instantiate nested beans when they are null. So, setting a.'b.c' will
     * instantiate a new 'b' and then set 'c' on it when 'b' is null. This is not possible when the property name is not the quoted nested path,
     * since a.b.c will result in a.getB().setC(), which is different from "a.set'B.C'()", where we know that the nested path is used on a setter.
     */
    void setProperty(String name, Object value) {
    	if (wrapperProperties.contains(name)) {
    		this.metaClass.setProperty(this, name, value)
    		return
    	}
    	if (value != null) {
	    	Class type = beanWrapper.getPropertyType(name)
	        if (value instanceof GString) value = value.toString()
	        PropertyEditor editor = findCustomEditor(type, name)
	        if (editor && value instanceof String) {
	        	editor.setAsText(value)
	        	value = editor.value
	        }
    	}
    	try {
    		beanWrapper.setPropertyValue(name, value)
    	} catch (NullValueInNestedPathException e) {
    		//There was a null value on the path
        	List path = name.tokenize('.')
        	Object bean = getWrappedInstance()
        	path.each { part ->
        		//Fill the path
        		if (bean[part] == null) {
        			Class type = GCU.getPropertyType(bean.class, part)
        			bean[part] = type.newInstance()
        		}
        		bean = bean[part]
        	}
    		//Then try again
    		beanWrapper.setPropertyValue(name, value)
    	}
    }
    
    /**
     * When invoking an unknown method, forward it to the bean
     */
    Object methodMissing(String name, args) {
    	Object bean = getWrappedInstance()
    	bean."$name"(*args)
    }
    
    String toString() {
        "WrappedBean: ${beanWrapper.wrappedInstance}"
    }
}
