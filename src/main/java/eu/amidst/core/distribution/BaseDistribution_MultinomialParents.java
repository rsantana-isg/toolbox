package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_BaseDistribution_MultinomialParents;
import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.EF_Distribution;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by andresmasegosa on 24/02/15.
 */
public class BaseDistribution_MultinomialParents<E extends Distribution> extends ConditionalDistribution {

    /**
     * The list of multinomial parents
     */
    private List<Variable> multinomialParents;

    private List<Variable> nonMultinomialParents;

    private List<E> baseDistributions;

    private boolean isBaseConditionalDistribution;

    public BaseDistribution_MultinomialParents(Variable var_, List<Variable> parents_) {

        this.var = var_;
        this.multinomialParents = new ArrayList<Variable>();
        this.nonMultinomialParents = new ArrayList<Variable>();
        this.parents = parents_;

        for (Variable parent : parents) {

            if (parent.isMultinomial()) {
                this.multinomialParents.add(parent);
            } else {
                this.nonMultinomialParents.add(parent);
            }
        }

        int size = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);
        this.baseDistributions = new ArrayList(size);

        if (nonMultinomialParents.size()==0){
            this.isBaseConditionalDistribution=false;
            for (int i = 0; i < size; i++) {
                this.baseDistributions.add((E)this.var.newUnivariateDistribution());
            }
        }else{
            this.isBaseConditionalDistribution=true;
            for (int i = 0; i < size; i++) {
                this.baseDistributions.add((E)this.var.newConditionalDistribution(this.nonMultinomialParents));
            }
        }


        //Make them unmodifiable
        this.multinomialParents = Collections.unmodifiableList(this.multinomialParents);
        this.nonMultinomialParents = Collections.unmodifiableList(this.nonMultinomialParents);
        this.parents = Collections.unmodifiableList(this.parents);
    }

    public List<Variable> getMultinomialParents() {
        return multinomialParents;
    }

    public List<Variable> getNonMultinomialParents() {
        return nonMultinomialParents;
    }

    public boolean isBaseConditionalDistribution() {
        return isBaseConditionalDistribution;
    }

    public int getNumberOfBaseDistributions(){
        return this.baseDistributions.size();
    }
    public E getBaseDistribution(Assignment parentAssignment){
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.multinomialParents, parentAssignment);
        return baseDistributions.get(position);
    }

    public void setBaseDistribution(Assignment parentAssignment, E baseDistribution) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents, parentAssignment);
        this.setBaseDistribution(position,baseDistribution);
    }

    public void setBaseDistribution(int position, E baseDistribution){
        this.baseDistributions.set(position, baseDistribution);
    }

    public E getBaseDistribution(int position){
        return this.baseDistributions.get(position);
    }

    public List<E> getBaseDistributions() {
        return baseDistributions;
    }

    public ConditionalDistribution getBaseConditionalDistribution(int position) {
        return (ConditionalDistribution)this.getBaseDistribution(position);
    }

    public UnivariateDistribution getBaseUnivariateDistribution(int position) {
        return (UnivariateDistribution)this.getBaseDistribution(position);
    }

    @Override
    public double getLogConditionalProbability(Assignment assignment) {
        return this.getBaseDistribution(assignment).getLogProbability(assignment);
    }

    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        if (this.isBaseConditionalDistribution)
            return ((ConditionalDistribution)this.getBaseDistribution(assignment)).getUnivariateDistribution(assignment);
        else
            return (UnivariateDistribution)this.getBaseDistribution(assignment);
    }

    @Override
    public int getNumberOfFreeParameters() {
        return this.baseDistributions.stream().mapToInt(dist -> dist.getNumberOfFreeParameters()).sum();
    }

    @Override
    public String label() {
        if (this.getConditioningVariables().size() == 0 || this.multinomialParents.size() == 0) {
            return this.getBaseDistribution(0).label();
        } else if (!this.isBaseConditionalDistribution) {
            return this.getBaseDistribution(0).label() + "| Multinomial";
        } else {
            return this.getBaseDistribution(0).label() + ", Multinomial";
        }
    }

    @Override
    public void randomInitialization(Random random) {
        this.baseDistributions.stream().forEach(dist -> dist.randomInitialization(random));
    }

    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        BaseDistribution_MultinomialParents<E> newdist = (BaseDistribution_MultinomialParents<E>)dist;

        int size = this.getNumberOfBaseDistributions();

        if (newdist.getNumberOfBaseDistributions()!=size)
            return false;

        for (int i = 0; i < size; i++) {
            if (!this.getBaseDistribution(i).equalDist(newdist.getBaseDistribution(i), threshold))
                return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("");
        for (int i = 0; i < this.getNumberOfBaseDistributions(); i++) {
            str.append(this.getBaseDistribution(i).toString());
            if (getNumberOfBaseDistributions() > 1 && i < getNumberOfBaseDistributions() - 1) {
                str.append("\n");
            }
        }
        return str.toString();
    }

    @Override
    public EF_BaseDistribution_MultinomialParents<? extends EF_Distribution> toEFConditionalDistribution(){

        if (this.isBaseConditionalDistribution()){
            List<EF_ConditionalDistribution> base_ef_dists = new ArrayList<>();

            for (int i = 0; i < this.getNumberOfBaseDistributions(); i++) {
                base_ef_dists.add(this.getBaseConditionalDistribution(i).toEFConditionalDistribution());
            }

            return new EF_BaseDistribution_MultinomialParents<>(this.multinomialParents,base_ef_dists);

        }else{
            List<EF_UnivariateDistribution> base_ef_dists = new ArrayList<>();

            for (int i = 0; i < this.getNumberOfBaseDistributions(); i++) {
                base_ef_dists.add(this.getBaseUnivariateDistribution(i).toEFUnivariateDistribution());
            }

            return new EF_BaseDistribution_MultinomialParents<>(this.multinomialParents,base_ef_dists);
        }
    }
}