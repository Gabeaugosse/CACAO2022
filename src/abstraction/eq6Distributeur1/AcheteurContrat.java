package abstraction.eq6Distributeur1;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import abstraction.eq8Romu.contratsCadres.Echeancier;
import abstraction.eq8Romu.contratsCadres.ExemplaireContratCadre;
import abstraction.eq8Romu.contratsCadres.IAcheteurContratCadre;
import abstraction.eq8Romu.contratsCadres.IVendeurContratCadre;
import abstraction.eq8Romu.contratsCadres.SuperviseurVentesContratCadre;
import abstraction.eq8Romu.filiere.Filiere;
import abstraction.eq8Romu.general.Journal;
import abstraction.eq8Romu.produits.ChocolatDeMarque;

public class AcheteurContrat extends DistributeurChocolatDeMarque implements IAcheteurContratCadre{//leorouppert
	protected Journal journalNegociationCC;
	protected Journal journalSuiviCC;
	protected List<ExemplaireContratCadre> mesContrats;
	private final double SEUIL_DELTA_ECHEANCE_PROPOSEE = 0.7;
	private final double SEUIL_AJOUT_ECHEANCE = 0.3;
	private final int STEP_INTERSECTION_MIN = 6;
	private final double PRIX_LIMITE = 1.2;
	
	public AcheteurContrat() {
		super();
		mesContrats = new ArrayList<ExemplaireContratCadre>();
		journalNegociationCC = new Journal("Negociations CC", this);
		journalSuiviCC = new Journal("Suivi des livraisons des CC", this);
	}


	/**
	 * @author Nathan,
	 * @return La liste des journaux renvoyé par distributeurChocolatDeMarque en ajoutant le journal pour les contrats cadre
	 */
	@Override
	public List<Journal> getJournaux() {
		List<Journal> l = super.getJournaux();
		l.add(journalNegociationCC);
		l.add(journalSuiviCC);
		return l;
	}
	/**
	 * @author Leo, Emma, Nathan
	 */
	@Override
	public boolean achete(Object produit) {
		if (NotreStock.seuilSecuFaillite() == false || ! (produit instanceof ChocolatDeMarque)) {
			return false;
		}
		if (partDuMarcheVoulu( ((ChocolatDeMarque)produit).getChocolat()) > 0.0) {
			return true;
		}
		return false;
	}

	public int[] interesctionEtapes(Echeancier e1, Echeancier e2) {
		int[] res = {0, 0};
		res[0] = (e1.getStepDebut() < e2.getStepDebut()) ? e2.getStepDebut() : e1.getStepDebut();
		res[1] = (e1.getStepFin() < e2.getStepFin()) ? e1.getStepFin() : e2.getStepFin();
		if (res[0] > res[1]) {
			return null;
		}
		return res;
	}

	public double echenacierDelta(Echeancier e1, Echeancier e2) {
		int[] intersection = interesctionEtapes(e1, e2);
		if (intersection == null || intersection[1] - intersection[0] < STEP_INTERSECTION_MIN) {
			return 1.0;
		}
		double delta = 0.0;
		for (int i = intersection[0]; i <= intersection[1]; i++) {
			if (e1.getQuantite(i) > e2.getQuantite(i)) {
				return -1.0;
			}
			delta += Math.abs(e2.getQuantite(i) - e1.getQuantite(i));
		}
		return (delta / getSomme(intersection[0], intersection[1], e2));
	}
	
	@Override
	public Echeancier contrePropositionDeLAcheteur(ExemplaireContratCadre contrat) {
		journalNegociationCC.ajouter("--> Contre propisiton du vendeur: voici l'echeancier proposée et celui proposée par le vendeur");
		Echeancier voulu = nouveauxEcheanciersVoulus().get(contrat.getProduit());
		journalNegociationCC.ajouter("--> " + voulu);
		journalNegociationCC.ajouter("--> " + contrat.getEcheancier());
		if (voulu == null) return null;
		double delta = echenacierDelta(contrat.getEcheancier(), voulu);
		journalNegociationCC.ajouter("--> Delta de ce qu'on voulait de: " + delta);
		if (delta != -1.0){ // TODO: Pour l'instant on s'assure juste qu'il n'y a pas de surplus
			journalNegociationCC.ajouter("--> Nous acceptons son écheancier");
			return contrat.getEcheancier();
		}
		journalNegociationCC.ajouter("Nous refusons son écheancier.");
		return null;
	}

	@Override
	public double contrePropositionPrixAcheteur(ExemplaireContratCadre contrat) {
		if (contrat.getPrix() > PRIX_LIMITE * 7.5* facteurPrixChocolat(((ChocolatDeMarque) contrat.getProduit()).getChocolat())) {
			double res = 7.5 * facteurPrixChocolat(((ChocolatDeMarque) contrat.getProduit()).getChocolat());
			journalNegociationCC.ajouter(contrat.getPrix() + " le kilo est trop élevé nous proposons " + res);
			return res;
		}
		else {
			journalNegociationCC.ajouter("--> Nous acceptons le prix proposé qui est " + contrat.getPrix());
			return contrat.getPrix();
		}
	}

	@Override
	public void notificationNouveauContratCadre(ExemplaireContratCadre contrat) {
		journalNegociationCC.ajouter(Color.GREEN, Color.BLACK, "Negociation réussie pour le contrat");
		this.setPrixVente((ChocolatDeMarque)contrat.getProduit(), contrat.getPrix());
		this.mesContrats.add(contrat);
	}

	@Override
	public void receptionner(Object produit, double quantite, ExemplaireContratCadre contrat) {
		double qteAttendu = contrat.getQuantiteALivrerAuStep();
		if (quantite != qteAttendu) {
			journalSuiviCC.ajouter(Color.RED, Color.BLACK, "Il manque " + (qteAttendu - quantite) + "Kg de ce que nous etions censé recevoir de " + contrat.getVendeur().getNom() + " pour le contrat #" + contrat.getNumero());
		}
		else {
			journalSuiviCC.ajouter("La quantité attendu (" + quantite + ") a été recu de " + contrat.getVendeur().getNom() + " pour le contrat #" + contrat.getNumero());
		}
		this.getNotreStock().addQte((ChocolatDeMarque) produit, quantite);
		this.setPrixVente((ChocolatDeMarque) produit, contrat.getPrix());
	}

	private void suppAnciensContrats() {//leorouppert
		List<ExemplaireContratCadre> aSupprimer = new ArrayList<ExemplaireContratCadre>();
		for (ExemplaireContratCadre contrat : mesContrats) {
			if (contrat.getQuantiteRestantALivrer() == 0.0 && contrat.getMontantRestantARegler() == 0.0) {
				aSupprimer.add(contrat);
			}
		}
		mesContrats.removeAll(aSupprimer);		
	}

	/**
	 * @author Nathan
	 * Permet d'avoir un echenacier additionnant tous les echeancier d'un chocolat 
	 * @return un echeancier de ce qu'il manque à fournir dans nos prévision
	 */
	public Map<ChocolatDeMarque, Echeancier> getEchenaceParChoco() {
		Map<ChocolatDeMarque, Echeancier> res = new HashMap<ChocolatDeMarque, Echeancier>();
		for (ChocolatDeMarque choco : Filiere.LA_FILIERE.getChocolatsProduits()) {
			res.put(choco, new Echeancier());
		}
		for (ExemplaireContratCadre ecd : mesContrats) {
			ChocolatDeMarque cm = (ChocolatDeMarque) ecd.getProduit();
			Echeancier e = ecd.getEcheancier();
			int start = (e.getStepDebut() < Filiere.LA_FILIERE.getEtape()+1) ? Filiere.LA_FILIERE.getEtape()+1 : e.getStepDebut();
			for (int i = start; i < e.getStepFin(); i++) {
				res.get(cm).set(i, res.get(cm).getQuantite(i) + e.getQuantite(i));
			}
		}
		return res;
	}

	public double getSomme(int stepDebut, int stepFin, Echeancier e) {
		double res = 0;
		for (int i = stepDebut; i <= stepFin; i++) {
			res += e.getQuantite(i);
		}
		return res;
	}

	public double attenduNProchainesEtapes(int n, ChocolatDeMarque choco) {
		double res = 0;
		int ajd = Filiere.LA_FILIERE.getEtape();
		for (int i = ajd+1; i <= n+ajd; i++) {
			res += Filiere.LA_FILIERE.getVentes(choco, i-24);
		}
		return res;
	}

	private Echeancier createEcheancier(Echeancier aCombler, int stepDebut, ChocolatDeMarque c) {
		Echeancier e = new Echeancier(stepDebut);
		for (int i = stepDebut; i < stepDebut + 24; i++) {
			double aComblerI = (aCombler == null) ? 0 : aCombler.getQuantite(i);
			e.ajouter(getPartMarque(c) * partCC*Filiere.LA_FILIERE.getVentes(c, i-24) * partDuMarcheVoulu(c.getChocolat()) - aComblerI);
		}
		return e;
	}

	

	public Map<ChocolatDeMarque, Echeancier> nouveauxEcheanciersVoulus() {
		Map<ChocolatDeMarque, Echeancier> res = new HashMap<ChocolatDeMarque, Echeancier>();
		Map<ChocolatDeMarque, Echeancier> echeancierTotal = getEchenaceParChoco();
		for (ChocolatDeMarque choco : Filiere.LA_FILIERE.getChocolatsProduits()) {
			Echeancier eChoco = echeancierTotal.get(choco);
			Echeancier e = createEcheancier(eChoco, Filiere.LA_FILIERE.getEtape()+1, choco);
			if (e.getQuantiteTotale() >= SuperviseurVentesContratCadre.QUANTITE_MIN_ECHEANCIER && e.getQuantiteTotale() > SEUIL_AJOUT_ECHEANCE*attenduNProchainesEtapes(24, choco)*partDuMarcheVoulu(choco.getChocolat())*partCC*getPartMarque(choco)) {
				// Sinon cela ne sert à rien de faire un nouveau contrat cadre
				res.put(choco, e);
			}
		}
		return res;
	}


	/**
	 * @author Nathan
	 */
	@Override
	public void next() {
		super.next();
		this.suppAnciensContrats();
		Map<ChocolatDeMarque, Echeancier> aAjouter = nouveauxEcheanciersVoulus();
		for (ChocolatDeMarque choco : Filiere.LA_FILIERE.getChocolatsProduits()) {
			for (IVendeurContratCadre vendeur : supCCadre.getVendeurs(choco)) {
				Echeancier aAjouterChoco = aAjouter.get(choco);
				if (aAjouterChoco != null) {
					journalNegociationCC.ajouter(Color.CYAN, Color.BLACK, "Nouvelle demande de CC avec " + vendeur.getNom() + " pour l'écheancier " + aAjouterChoco);
					ExemplaireContratCadre ecc = supCCadre.demandeAcheteur((IAcheteurContratCadre)this, vendeur, choco, aAjouterChoco, this.cryptogramme, false);
					if (ecc != null) {
						mesContrats.add(ecc);
						aAjouter = nouveauxEcheanciersVoulus();
						System.out.println(ecc.getPrix());
						this.setPrixVente(choco, ecc.getPrix());
						journalNegociationCC.ajouter(Color.GREEN, Color.BLACK, "La négociation a abouti");
					}
					else {
						journalNegociationCC.ajouter(Color.RED, Color.BLACK, "La negociation a echouée.");
					}
				}
			}
		}
	}
}

