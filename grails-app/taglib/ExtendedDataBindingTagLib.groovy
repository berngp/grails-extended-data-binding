import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GAA
import org.springframework.beans.TypeMismatchException
import org.springframework.validation.DataBinder

class ExtendedDataBindingTagLib {
    
	private getController(request) {
		request.getAttribute(GAA.CONTROLLER)
	}
	
    /**
     * Tag used to wrap a bean to allow data conversion to String according to registered PropertyEditors
     */
    def wrap = { attrs ->
        def bean = attrs.bean
        def var = attrs.var
        if (bean == null) throwTagError("[g:wrap] tag requires attribute: [bean]")
        if (var == null || var.empty) throwTagError("[g:wrap] tag requires attribute: [var]")
        def wrappedBean = getController(request).wrapBean(bean)
        switch (attrs.scope) {
            case "application":
                servletContext[var] = wrappedBean
                break
            case "session":
                session[var] = wrappedBean
                break
            case "request":
                request[var] = wrappedBean
                break
            default:
                pageScope[var] = wrappedBean
                break
        }
    }
    
    /**
     * Tag that works just like &lt;g:each&gt;, wrapping each element 
     */
    def eachWrapped = { attrs, body ->
    	def collection = attrs."in"
    	if (collection == null) throwTagError("Tag [eachWrapped] is missing required attribute [in]")
    	def var = attrs.var ?: "it"
    	def status = attrs.status
		def wrapper = null
		collection.eachWithIndex { bean, index ->
			if (wrapper == null) {
				wrapper = getController(request).wrapBean(bean)
			} else {
				wrapper.setWrappedInstance(bean)
			}
			def model = [(var):wrapper]
			if (status) {
				model[status] = index
			}
			out << body(model)
		}
    }
}
