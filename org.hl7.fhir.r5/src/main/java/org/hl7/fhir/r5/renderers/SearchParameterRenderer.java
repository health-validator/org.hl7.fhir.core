package org.hl7.fhir.r5.renderers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Enumeration;
import org.hl7.fhir.r5.model.OperationDefinition;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.SearchParameter;
import org.hl7.fhir.r5.model.SearchParameter.SearchComparator;
import org.hl7.fhir.r5.model.SearchParameter.SearchModifierCode;
import org.hl7.fhir.r5.model.SearchParameter.SearchParameterComponentComponent;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.renderers.utils.RenderingContext;
import org.hl7.fhir.r5.renderers.utils.Resolver.ResourceContext;
import org.hl7.fhir.r5.utils.EOperationOutcome;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

public class SearchParameterRenderer extends TerminologyRenderer {

  public SearchParameterRenderer(RenderingContext context) {
    super(context);
  }

  public SearchParameterRenderer(RenderingContext context, ResourceContext rcontext) {
    super(context, rcontext);
  }
  
  public boolean render(XhtmlNode x, Resource dr) throws IOException, FHIRException, EOperationOutcome {
    return render(x, (SearchParameter) dr);
  }

  public boolean render(XhtmlNode x, SearchParameter spd) throws IOException, FHIRException, EOperationOutcome {
    x.h2().addText(spd.getName());
    XhtmlNode p =  x.para();
    p.tx("Parameter ");
    p.code().tx(spd.getCode());
    p.tx(":");
    p.code().tx(spd.getType().toCode());
    addMarkdown(x, spd.getDescription());

    XhtmlNode tbl = x.table("grid");
    XhtmlNode tr = tbl.tr();
    tr.td().tx(Utilities.pluralize("Resource", spd.getBase().size()));
    XhtmlNode td = tr.td();
    for (CodeType t : spd.getBase()) {
      StructureDefinition sd = context.getWorker().fetchTypeDefinition(t.toString());
      if (sd != null && sd.hasUserData("path")) {
        td.ah(sd.getUserString("path")).sep(", ").tx(t.getCode());
      } else {
        td.sep(", ").tx(t.getCode());
      }
    }
    tr = tbl.tr();
    tr.td().tx("Expression");
    if (spd.hasExpression()) {
      tr.td().code().tx(spd.getExpression());
    } else {
      tr.td().tx("(none)");
    }
    if (spd.hasProcessingMode()) {
      tr = tbl.tr();
      tr.td().tx("Usage");
      tr.td().tx(spd.getProcessingMode().getDisplay());      
    }
    if (spd.hasTarget()) {
      tr = tbl.tr();
      tr.td().tx(Utilities.pluralize("Target Resources", spd.getTarget().size()));
      td = tr.td();
      if (isAllConcreteResources(spd.getTarget())) {
        td.ah(Utilities.pathURL(context.getSpecificationLink(), "resourcelist.html")).tx("All Resources");
      } else {
        for (CodeType t : spd.getTarget()) {
          StructureDefinition sd = context.getWorker().fetchTypeDefinition(t.toString());
          if (sd != null && sd.hasUserData("path")) {
            td.sep(", ");
            td.ah(sd.getUserString("path")).tx(t.getCode());
          } else {
            td.sep(", ");
            td.tx(t.getCode());
          }
        }
      }
    }
    tr = tbl.tr();
    tr.td().tx("Multiples");
    if (spd.getMultipleAnd() && spd.getMultipleOr()) {
      tr.td().tx("The parameter can repeat (and) and can have repeating values (or)");      
    } else if (spd.getMultipleOr()) {
      tr.td().tx("The parameter can repeat (and) but each repeat can only have one value");      
    } else if (spd.getMultipleAnd()) {
      tr.td().tx("The parameter cannot repeat (and) but the single parameter can have multiple values (or)");      
    } else { 
      tr.td().tx("The parameter cannot repeat or have multiple values");
    }
    if (spd.hasComparator()) {
      tr = tbl.tr();
      tr.td().tx("Comparators");
      td = tr.td();
      td.tx("Allowed: ");
      for (Enumeration<SearchComparator> t : spd.getComparator()) {
        td.sep(", ");
        td.tx(t.asStringValue());
      }      
    }
    if (spd.hasModifier()) {
      tr = tbl.tr();
      tr.td().tx("Modifiers");
      td = tr.td();
      td.tx("Allowed: ");
      for (Enumeration<SearchModifierCode> t : spd.getModifier()) {
        td.sep(", ");
        td.tx(t.asStringValue());
      }      
    }
    if (spd.hasChain()) {
      tr = tbl.tr();
      tr.td().tx("Chains");
      td = tr.td();
      td.tx("Allowed: ");
      for (StringType t : spd.getChain()) {
        td.sep(", ");
        td.tx(t.asStringValue());
      }      
    }
    
    if (spd.hasComponent()) {
      x.para().b().tx("Components");
      tbl = x.table("grid");
      for (SearchParameterComponentComponent t : spd.getComponent()) {
        tr = tbl.tr();
        SearchParameter tsp = context.getWorker().fetchResource(SearchParameter.class, t.getDefinition());
        if (tsp != null && tsp.hasUserData("path")) {
          tr.td().ah(tsp.getUserString("path")).tx(tsp.present());          
        } else {
          tr.td().tx(t.getDefinition());
        }
        tr.td().code().tx(t.getExpression());
      }
    }
    return false;
  }

  private boolean isAllConcreteResources(List<CodeType> target) {
    for (String s : context.getWorker().getResourceNames()) {
      StructureDefinition sd = context.getWorker().fetchTypeDefinition(s);
      if (!sd.getAbstract() && !Utilities.existsInList(sd.getType(), "Parameters")) {
        boolean found = false;
        for (CodeType c : target) {
          found = found || sd.getName().equals(c.getValue());
        }
        if (!found) {
          return false;
        }
      }
    }
    return true;
  }

  public void describe(XhtmlNode x, OperationDefinition opd) {
    x.tx(display(opd));
  }

  public String display(OperationDefinition opd) {
    return opd.present();
  }

  @Override
  public String display(Resource r) throws UnsupportedEncodingException, IOException {
    return ((OperationDefinition) r).present();
  }

}
