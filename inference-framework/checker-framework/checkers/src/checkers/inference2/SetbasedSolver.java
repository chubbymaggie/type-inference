package checkers.inference2;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import checkers.inference2.Constraint.EqualityConstraint;
import checkers.inference2.Constraint.SubtypeConstraint;
import checkers.inference2.Constraint.UnequalityConstraint;
import checkers.inference2.Reference.AdaptReference;
import static com.esotericsoftware.minlog.Log.*;

public class SetbasedSolver extends AbstractConstraintSolver<InferenceChecker> {
	
	protected Set<Constraint> worklist = new LinkedHashSet<Constraint>();

	protected Map<Integer, Set<Constraint>> refToConstraints = new HashMap<Integer, Set<Constraint>>();

	protected Map<String, List<Constraint>> adaptRefToConstraints; 

	protected Map<Integer, Set<Reference>> declRefToContextRefs; 
	
	public SetbasedSolver(InferenceChecker t) {
		super(t);
		adaptRefToConstraints = new HashMap<String, List<Constraint>>();
		declRefToContextRefs = new HashMap<Integer, Set<Reference>>();
	}
	
	protected void buildRefToConstraintMapping(Constraint c) {
        Reference left = null, right = null; 
        if (c instanceof SubtypeConstraint
                || c instanceof EqualityConstraint
                || c instanceof UnequalityConstraint) {
            left = c.getLeft();
            right = c.getRight();
        }
        if (left != null && right != null) {
            Reference[] refs = {left, right};
            for (Reference ref : refs) {
                List<Reference> avs = new ArrayList<Reference>(2);
                if (ref instanceof AdaptReference) {
                    Reference decl = ((AdaptReference) ref).getDeclRef();
                    Reference context = ((AdaptReference) ref).getContextRef();
                    avs.add(decl);
                    avs.add(context);
                    
                    String key = ref.getName();
                    List<Constraint> l = adaptRefToConstraints.get(key);
                    if (l == null) {
                        l = new ArrayList<Constraint>(2);
                        adaptRefToConstraints.put(key, l);
                    }
                    l.add(c);
                    Set<Reference> contextSet = declRefToContextRefs.get(decl.getId());
                    if (contextSet == null) {
                        contextSet = new HashSet<Reference>();
                        declRefToContextRefs.put(decl.getId(), contextSet);
                    }
                    contextSet.add(((AdaptReference) ref).getContextRef());
                } else
                    avs.add(ref);
                for (Reference av : avs) {
                    Set<Constraint> set = refToConstraints.get(av.getId());
                    if (set == null) {
                        set = new LinkedHashSet<Constraint>();
                        refToConstraints.put(av.getId(), set);
                    }
                    set.add(c);
                }
            }
        }
        if ((c instanceof SubtypeConstraint)
                && !(left instanceof AdaptReference)
                && !(right instanceof AdaptReference)) {
            left.addGreaterConstraint(c);
            right.addLessConstraint(c);
        }
    }
	
	private void buildRefToConstraintMapping(Set<Constraint> cons) {
		for (Constraint c : cons) {
            buildRefToConstraintMapping(c);
		}
	}

	@Override
	protected boolean setAnnotations(Reference av, Set<AnnotationMirror> annos) 
			throws SolverException {
        Set<AnnotationMirror> oldAnnos = av.getAnnotations(checker);
		if (av instanceof AdaptReference)
			return setAnnotations((AdaptReference) av, annos);
		if (oldAnnos.equals(annos))
			return false;

        Set<Constraint> relatedConstraints = refToConstraints.get(av.getId());
        if (relatedConstraints != null) {
            worklist.addAll(relatedConstraints);
        }

        return super.setAnnotations(av, annos);
    }

	@Override
	protected Set<Constraint> solveImpl() {
        Set<Constraint> constraints = checker.getConstraints();
		BitSet warnConstraints = new BitSet(Constraint.maxId());
        buildRefToConstraintMapping(constraints);
        worklist.addAll(constraints);
        Set<Constraint> conflictConstraints = new LinkedHashSet<Constraint>();
        while(!worklist.isEmpty()) {
            Iterator<Constraint> it = worklist.iterator();
            Constraint c = it.next();
            it.remove();
            try {
                handleConstraint(c);
            } catch (SolverException e) {
                FailureStatus fs = checker.getFailureStatus(c);
                if (fs == FailureStatus.ERROR) {
                    conflictConstraints.add(c);
                } else if (fs == FailureStatus.WARN) {
                    if (!warnConstraints.get(c.getId())) {
                        warn(this.getClass().getSimpleName(), "Failed handling constraint " + c);
                        warnConstraints.set(c.getId());
                    }
                }
            }
        }
        return conflictConstraints;
	}

}
