import org.codehaus.groovy.grails.web.binding.GrailsDataBinder
import org.springframework.beans.BeanWrapperImpl
import org.springframework.validation.DataBinder
import org.springframework.validation.BindingResult 
import br.inf.freeit.extendeddatabinding.WrappedBean

class ExtendedDataBindingGrailsPlugin {
    def version = 0.3
    def observe = ['controllers']
    def author = "Luis Fernando Planella Gonzalez"
    def authorEmail = "lfpg.dev@gmail.com"
    def title = "This plugin extends Grails' data binding possibilities, both for parsing the user input and to present formatted data"
    def description = '''\
The extended data binding plugin aims to allow configuring the DataBinder which controllers will use to parse
the user input and populate objects, as well as using a custom class, WrappedBean to format data as String.
This configuration can be done application-wide by setting closures on the servlet context 
(possibly on the ApplicationBootStrap) on the following attributes: newDataBinder (takes the request and the 
object and should return a GrailsDataBinder instance) and newBeanWrapper (also takes the request and the 
object and should return a Spring's BeanWrapper instance).
This plugin adds some methods to controllers: getBinder (takes an object and returns a DataBinder for that object, setting
it on the request under the attribute 'dataBinder'), wrapBean (takes an object and returns a WrappedBean instance - 
a class that uses an Spring's BeanWrapper and converts properties to strings using registered PropertyEditors) and bind 
(takes an object, performs the data binding and returns the object itself). Both getBinder and wrapBean methods invoke 
the global newDataBinder or newBeanWrapper closures (as explained above) and try calling a registerCustomEditors method 
(passing the object) on the controller, which may be used to set any specific PropertyEditors on either DataBinder or 
BeanWrapper). The getBinder also tries caling a initBinder method on the controller after registerCustomEditors. 
Also, a tag library adds the following tags to the 'g' namespace: wrap (takes a bean attribute and a var attribute,
exporting a WrappedBean instance into the given var - optionally under a custom scope) and eachWrapped (same as g:each,
but automatically wrapping each element)
'''
    def documentation = "http://www.grails.org/plugin/extended-data-binding"

    private boolean loggedDataBinder = false
    private boolean loggedBeanWrapper = false

    def doWithSpring = {
    }

    def doWithApplicationContext = { applicationContext ->
    }

    def doWithWebDescriptor = { xml ->
    }

    def doWithDynamicMethods = { ctx ->
        application.controllerClasses.each { controllerClass ->
            addMethods(ctx, controllerClass)
        }
    }

    def onChange = { event ->
        if (event.application.isControllerClass(event.source)) {
            addMethods(event.applicationContext, event.source)
        }
    }

    def onApplicationChange = { event ->
    }

    def addMethods = { ctx, controllerClass -> 

        // Binds the current request to the given object, returning the object itself
        controllerClass.metaClass.bind = { object ->
        	if (object == null) return null
        	DataBinder dataBinder = getBinder(object)
        	dataBinder.bind(params)
        	BindingResult result = dataBinder.bindingResult
        	try {
        		object.setErrors(result)
        	} catch (Exception e) {
        		//Ignore
        	}
            object
        }

        // The getDataBinder method will return the DataBinder instance, storing it on the request on the 'dataBinder' attribute
        controllerClass.metaClass.getBinder = { object ->
            def binder = null
            try {
            	def closure = ctx.servletContext.getAttribute('newDataBinder')
            	if (closure != null) {
            		binder = closure(request, object)
            	}
            } catch (Exception ex) {
            	if (!loggedDataBinder) {
            		log.warn("Error invoking closure under servletContext['newDataBinder']", ex)
            	}
            }
            if (!(binder instanceof GrailsDataBinder)) {
            	if (!loggedDataBinder) {
            		log.info("No global configuration for DataBinders. Assuming defaults.")
            		loggedDataBinder = true
            	}
                binder = GrailsDataBinder.createBinder(object, GrailsDataBinder.DEFAULT_OBJECT_NAME, request)
            }
            try {
                registerCustomEditors(binder)
            } catch (Exception ex) {}
            try {
                initBinder(binder)
            } catch (Exception ex) {}
            request.setAttribute("dataBinder", binder)
            return binder
        }

        // Wraps the given bean on a BeanWrapper
        controllerClass.metaClass.wrapBean = { object ->
        	if (object instanceof WrappedBean) return object
        	
            def beanWrapper = null
            try {
            	def closure = ctx.servletContext.getAttribute('newBeanWrapper')
            	beanWrapper = closure(request, object)
            } catch (Exception ex) {
            	if (!loggedBeanWrapper) {
            		log.warn("Error invoking closure under servletContext['newBeanWrapper']", ex)
            	}
            }
        	if (!beanWrapper) {
        		if (!loggedBeanWrapper) {
        			log.info("No global configuration for BeanWrappers. Assuming defaults.")
	        		loggedBeanWrapper = true
        		}
        		beanWrapper = new BeanWrapperImpl()
        	}
            beanWrapper.setWrappedInstance(object)
            try {
                registerCustomEditors(beanWrapper)
            } catch (Exception ex) {}
            return new WrappedBean(beanWrapper)
        }

    }
}
