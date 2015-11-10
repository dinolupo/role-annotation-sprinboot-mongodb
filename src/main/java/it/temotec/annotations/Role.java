/**
 * 
 */
package it.temotec.annotations;

import java.lang.annotation.Target;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * @author dino lupo
 *
 * Set this to verify if the current logged user has permissions to access this method
 *
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Role {	
	String[] value() default {"guest"};
}
