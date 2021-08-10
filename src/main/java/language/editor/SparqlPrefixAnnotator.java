package language.editor;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import language.psi.*;
import org.jetbrains.annotations.NotNull;
import settings.SparqlSettingsUtil;

import static language.psi.SparqlTypes.IRIREF;

public class SparqlPrefixAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // annotate prefixedname, if prefix hasn't been declared within the query
        // as some libraries require a prefix declaration even if the endpoint knows the prefix
        // and as some endpoints simply don't know the prefix and need the prefix declaration to fulfill the query
        if (element instanceof SparqlPrefixedName){
            SparqlPrefixedName prefixedName = (SparqlPrefixedName) element;
            // if prefix isn't declared within the query -> annotate
            if (SparqlPsiImplUtil.getPrefixMapping(prefixedName).getNsPrefixURI(prefixedName.getPrefix()) == null) {

                HighlightSeverity highlightSeverity = HighlightSeverity.WEAK_WARNING;
                // if it's a standard prefix, a declaration isn't necessary as the endpoint is supposed to know the prefix
                // Information tag --> no highlighting instead of weak warning
                if (SparqlSettingsUtil.getStandardPrefixes().getNsPrefixURI(prefixedName.getPrefix()) != null) {
                    highlightSeverity = HighlightSeverity.INFORMATION;
                }
                holder.newAnnotation(
                        highlightSeverity, "The prefix hasn't been declared within the query.")
                        .withFix(new SparqlCreatePrefixDeclQuickFix(prefixedName.getPrefix()))
                        .create();
            }
        }

        // annotate iri, if iri can be shortened with a prefix
        // SparqlIri is already excluding Iris in Prologue
        if (element instanceof SparqlIri && element.getNode().getChildren(TokenSet.create(IRIREF)).length == 1) {
            String iri = element.getNode().getChildren(TokenSet.create(IRIREF))[0].getText().replaceAll("[<>]","");
            // trying to short with query prefix declarations and standard prefixes
            String prefixedName = SparqlPsiImplUtil.getPrefixMapping(element).withDefaultMappings(SparqlSettingsUtil.getStandardPrefixes()).qnameFor(iri);
            if (prefixedName != null) {
                holder.newAnnotation(
                        HighlightSeverity.WEAK_WARNING, "The IRI can be shortened by " + prefixedName + ".")
                        .withFix(new SparqlShortenIriQuickFix(prefixedName, element))
                        .create();
            } else {
                // check if it can be shortened with non-standard prefix
                prefixedName = SparqlSettingsUtil.getStoredPrefixes().qnameFor(iri);
                if(prefixedName != null) {
                    holder.newAnnotation(
                            HighlightSeverity.INFORMATION, "The IRI can be shortened by " + prefixedName + ".")
                            .withFix(new SparqlShortenIriQuickFix(prefixedName, element))
                            .create();
                }
            }
        }

    }

}
