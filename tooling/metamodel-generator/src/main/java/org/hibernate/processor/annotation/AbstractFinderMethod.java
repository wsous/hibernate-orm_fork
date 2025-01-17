/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.processor.util.Constants;

import java.util.List;
import java.util.Locale;

import static org.hibernate.processor.util.Constants.HIB_SESSION;
import static org.hibernate.processor.util.StringUtil.getUpperUnderscoreCaseFromLowerCamelCase;

/**
 * @author Gavin King
 */
public abstract class AbstractFinderMethod extends AbstractQueryMethod  {
	final String entity;
	final List<String> fetchProfiles;

	AbstractFinderMethod(
			AnnotationMetaEntity annotationMetaEntity,
			String methodName,
			String entity,
			boolean belongsToDao,
			String sessionType,
			String sessionName,
			List<String> fetchProfiles,
			List<String> paramNames,
			List<String> paramTypes,
			List<OrderBy> orderBys,
			boolean addNonnullAnnotation,
			boolean convertToDataExceptions) {
		super( annotationMetaEntity,
				methodName,
				paramNames, paramTypes, entity,
				sessionType, sessionName,
				belongsToDao, orderBys,
				addNonnullAnnotation,
				convertToDataExceptions );
		this.entity = entity;
		this.fetchProfiles = fetchProfiles;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public boolean hasStringAttribute() {
		return false;
	}

	@Override
	public String getTypeDeclaration() {
		return entity;
	}

	@Override
	public String getAttributeNameDeclarationString() {
		return new StringBuilder()
				.append("public static final String ")
				.append(constantName())
				.append(" = \"!")
				.append(annotationMetaEntity.getQualifiedName())
				.append('.')
				.append(methodName)
				.append("(")
				.append(parameterList())
				.append(")")
				.append("\";")
				.toString();
	}

	String constantName() {
		return getUpperUnderscoreCaseFromLowerCamelCase(methodName) + "_BY_"
				+ paramNames.stream()
				.map(StringHelper::unqualify)
				.map(name -> name.toUpperCase(Locale.ROOT))
				.reduce((x,y) -> x + "_AND_" + y)
				.orElse("");
	}

	void comment(StringBuilder declaration) {
		declaration
				.append("\n/**")
				.append("\n * Find ")
				.append("{@link ")
				.append(annotationMetaEntity.importType(entity))
				.append("} by ");
		long paramCount = paramTypes.stream()
				.filter(type -> !isSpecialParam(type))
				.count();
		int count = 0;
		for (int i = 0; i < paramTypes.size(); i++) {
			String type = paramTypes.get(i);
			if ( !isSpecialParam(type) ) {
				if ( count>0 ) {
					if ( count + 1 == paramCount) {
						declaration
								.append(paramCount>2 ? ", and " : " and "); //Oxford comma
					}
					else {
						declaration
								.append(", ");
					}
				}
				count++;
				final String path = paramNames.get(i)
						.replace('$', '.');
				declaration
						.append("{@link ")
						.append(annotationMetaEntity.importType(entity))
						.append('#')
						.append(qualifier(path))
						.append(' ')
						.append(path)
						.append("}");
			}
		}
		declaration
				.append('.')
				.append("\n *");
		see( declaration );
//		declaration
//				.append("\n *");
//		for (String param : paramNames) {
//			declaration
//					.append("\n * @see ")
//					.append(annotationMetaEntity.importType(entity))
//					.append('#')
//					.append(param);
//		}
		declaration
				.append("\n **/\n");
	}

	String qualifier(String name) {
		final int index = name.indexOf('$');
		return index > 0 ? name.substring(0, index) : name;
	}

	void unwrapSession(StringBuilder declaration) {
		if ( isUsingEntityManager() ) {
			declaration
					.append(".unwrap(")
					.append(annotationMetaEntity.importType(HIB_SESSION))
					.append(".class)\n\t\t\t");
		}
	}

	boolean enableFetchProfile(StringBuilder declaration, boolean unwrapped) {
		if ( !fetchProfiles.isEmpty() ) {
			unwrapQuery( declaration, unwrapped );
			unwrapped = true;
		}
		for ( String profile : fetchProfiles ) {
			declaration
					.append("\t\t\t.enableFetchProfile(")
					.append(profile)
					.append(")\n");
		}
		return unwrapped;
	}

	void preamble(StringBuilder declaration) {
		modifiers( declaration );
		entityType( declaration );
		declaration
				.append(" ")
				.append(methodName);
		parameters( paramTypes, declaration ) ;
		declaration.append(" {\n");
	}

	void tryReturn(StringBuilder declaration) {
		if (dataRepository) {
			declaration
					.append("\ttry {\n\t");
		}
		declaration
				.append("\treturn ")
				.append(sessionName);
	}

	private void entityType(StringBuilder declaration) {
		if ( isReactive() ) {
			declaration
					.append(annotationMetaEntity.importType(Constants.UNI))
					.append('<');
		}
		declaration
				.append(annotationMetaEntity.importType(entity));
		if ( isReactive() ) {
			declaration
					.append('>');
		}
	}

	void modifiers(StringBuilder declaration) {
		declaration
				.append(belongsToDao ? "@Override\npublic " : "public static ");
	}
}
