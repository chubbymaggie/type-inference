/**
 * 
 */
package checkers.inference2.jcrypt.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

@Retention(RetentionPolicy.RUNTIME) 
@Documented
@TypeQualifier
@Inherited
@SubtypeOf({Top.class, Poly.class, Sensitive.class, Clear.class})
//@ImplicitFor(
//	types={TypeKind.NULL}
//    )
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Bottom {
    
}
