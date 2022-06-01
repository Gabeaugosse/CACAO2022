package abstraction.eq3Transformateur1;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import abstraction.eq8Romu.contratsCadres.Echeancier;
import abstraction.eq8Romu.contratsCadres.ExemplaireContratCadre;
import abstraction.eq8Romu.contratsCadres.ExempleTransformateurContratCadre;
import abstraction.eq8Romu.contratsCadres.IVendeurContratCadre;
import abstraction.eq8Romu.contratsCadres.SuperviseurVentesContratCadre;
import abstraction.eq8Romu.filiere.Filiere;
import abstraction.eq8Romu.filiere.IActeur;
import abstraction.eq8Romu.general.Journal;
import abstraction.eq8Romu.general.Variable;
import abstraction.eq8Romu.produits.Chocolat;
import abstraction.eq8Romu.produits.ChocolatDeMarque;

public class Transformateur1ContratCadreVendeur extends Transformateur1Bourse implements IVendeurContratCadre{
	
	protected List<ExemplaireContratCadre> mesContratEnTantQueVendeur;
	
	public Transformateur1ContratCadreVendeur() {
		super();
		this.mesContratEnTantQueVendeur=new LinkedList<ExemplaireContratCadre>();
	}
	
	// fonction qui détermine quel type de chocolat on vend en contrat cadre; auteur Julien */
	public boolean vend(Object produit) {
		journal.ajouter("debut CC avec distrib");

		if (produit instanceof ChocolatDeMarque){
			if ((((ChocolatDeMarque)produit).getChocolat()==Chocolat.MQ)
					||(((ChocolatDeMarque)produit).getChocolat()==Chocolat.MQ_BE)
					||(((ChocolatDeMarque)produit).getChocolat()==Chocolat.MQ_O)) {
				journalCC.ajouter("on veut bien vendre car WTF");
				return true;
			}
		}
		journalCC.ajouter("on ne veut pas vendre");
		return false;
	}

	// négocie un échancier inférieur à X échéances; auteur Julien */
	public Echeancier contrePropositionDuVendeur(ExemplaireContratCadre contrat) {
		if (contrat.getEcheancier().getNbEcheances()<100) {
			journalCC.ajouter("on aime cet échéancier");
			return contrat.getEcheancier();
		}
		journalCC.ajouter("on chie sur leurs échéances et on refuse car CC long terme");
		return null;
	}

	/** Prix initial = 1.4 x prixVenteMin 
	 *  Alexandre*/
	public double propositionPrix(ExemplaireContratCadre contrat) {
		journalCC.ajouter("ça négocie dur les prix");
		return this.prixVenteMin.get(((ChocolatDeMarque)contrat.getProduit()).getChocolat())*1.4;
	}

	// négocie une contreproposition du prix; auteur Julien */
	public double contrePropositionPrixVendeur(ExemplaireContratCadre contrat) {
		if (contrat.getPrix()>prixVenteMin.get(((ChocolatDeMarque)contrat.getProduit()).getChocolat())) {
			journalCC.ajouter("on aime ce prix");
			return contrat.getPrix();
		} else {
			journalCC.ajouter("ça négocie dur les prix");
			return Math.max(contrat.getPrix()+prixVenteMin.get(((ChocolatDeMarque)contrat.getProduit()).getChocolat())/2.0,0.0);
		}
		
	}

	/** Ajout du contrat dans la liste de contrats à honorer
	 *  et maj de dernierPrixVenteChoco
	 *  Alexandre*/
	public void notificationNouveauContratCadre(ExemplaireContratCadre contrat) {
		mesContratEnTantQueVendeur.add(contrat);
		
		//System.out.println(dernierPrixVenteChocoReset.getDistributeurs());
		//System.out.println(dernierPrixVenteChocoReset.getChocolats());
		//System.out.println(dernierPrixVenteChocoReset.getPrix(getDescription(), null));
		
		if (dernierPrixVenteChocoReset.getPrix(
				contrat.getAcheteur().getNom(), 
				((ChocolatDeMarque)contrat.getProduit()).getChocolat())
				> contrat.getPrix() 
				&& dernierPrixVenteChocoReset.getPrix(
						contrat.getAcheteur().getNom(), 
						((ChocolatDeMarque)contrat.getProduit()).getChocolat()) 
				!= 0) { // on garde le prix minimum negocie avec les vendeurs
			dernierPrixVenteChocoReset.setPrix(
					contrat.getAcheteur().getNom(), 
					((ChocolatDeMarque)contrat.getProduit()).getChocolat(), 
					contrat.getPrix());
			
		}journalCC.ajouter("nouveau contrat cadre vendeur");
		
	}

	// modification du stock ; auteur Julien */
	public double livrer(Object produit, double quantite, ExemplaireContratCadre contrat) {
		double livre = Math.min(stockChoco.get(((ChocolatDeMarque)contrat.getProduit()).getChocolat()), quantite);
		if (livre==quantite) {
			stockChoco.put(((ChocolatDeMarque)contrat.getProduit()).getChocolat(),stockChoco.get(((ChocolatDeMarque)contrat.getProduit()).getChocolat())-quantite);
			return quantite;
		}
		stockChoco.put(((ChocolatDeMarque)contrat.getProduit()).getChocolat(),1000.0);
		return livre;
	}
	
	public void next() {
		super.next();
		// Supprime les contrats obsolètes et honorés -Julien
		List<ExemplaireContratCadre> contratsObsoletes=new LinkedList<ExemplaireContratCadre>();
		for (ExemplaireContratCadre contrat : this.mesContratEnTantQueVendeur) {
			if (contrat.getQuantiteRestantALivrer()==0.0 && contrat.getMontantRestantARegler()==0.0) {
				contratsObsoletes.add(contrat);
			}
		}
		this.mesContratEnTantQueVendeur.removeAll(contratsObsoletes);
		journalCC.ajouter("test ContratCadreVendeur / Les contrats cadres vendeur obsolete ont ete supprime");
	}
	
	/** 
	 *  Alexandre*/
	public void initialiser() {
		super.initialiser();
	}
	

	
}
