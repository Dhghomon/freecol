/**
 *  Copyright (C) 2002-2013   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.border.Border;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.ProductionType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;


/**
 * Display the production of a unit.
 */
public class WorkProductionPanel extends FreeColPanel {

    private final Turn turn = getGame().getTurn();

    private static final Border border = BorderFactory
        .createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                                                              Color.BLACK),
                              BorderFactory.createEmptyBorder(2, 2, 2, 2));


    /**
     * Create a new production display.
     *
     * TODO: expand display to handle several outputs
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The <code>Unit</code> that is producing.
     */
    public WorkProductionPanel(FreeColClient freeColClient, Unit unit) {
        super(freeColClient, new MigLayout("wrap 3, insets 10 10 10 10",
                                           "[]30:push[right][]", ""));

        final ImageLibrary lib = getGUI().getImageLibrary();
        final Colony colony = unit.getColony();
        final UnitType unitType = unit.getType();
        final WorkLocation wl = (WorkLocation)unit.getLocation();
        final List<AbstractGoods> outputs = wl.getOutputs();
        final GoodsType goodsType = (outputs.isEmpty()) ? null
            : outputs.get(0).getType();

        List<Modifier> modifiers = new ArrayList<Modifier>();
        List<Modifier> moreModifiers = new ArrayList<Modifier>();
        List<Modifier> mods;
        String shortName = "";
        String longName = "";
        Image image = null;
        float result = (outputs.isEmpty()) ? 0.0f
            : outputs.get(0).getAmount();

        if (!outputs.isEmpty()) {
            if (wl instanceof ColonyTile) {
                final ColonyTile colonyTile = (ColonyTile)wl;
                mods = wl.getProductionModifiers(goodsType, unitType);
                if (FeatureContainer.applyModifiers(result, turn, mods) > 0) {
                    modifiers.addAll(mods);
                    mods = wl.getProductionModifiers(goodsType, null);
                    moreModifiers.addAll(mods);
                }

                final Tile tile = colonyTile.getWorkTile();
                final TileType tileType = tile.getType();
                shortName = Messages.getName(tileType);
                longName = Messages.message(colonyTile.getLabel());
                Image terrain = lib.getTerrainImage(tileType, tile.getX(),
                                                    tile.getY());
                image = new BufferedImage(terrain.getWidth(null),
                                          terrain.getHeight(null),
                                          BufferedImage.TYPE_INT_ARGB);
                getGUI().displayColonyTile((Graphics2D)image.getGraphics(),
                                           tile, colony);

            } else if (wl instanceof Building) {
                final Building building = (Building)wl;
                mods = wl.getProductionModifiers(goodsType, unitType);
                modifiers.addAll(mods);
                mods = wl.getProductionModifiers(goodsType, null);
                moreModifiers.addAll(mods);

                shortName = Messages.getName(building.getType());
                longName = shortName;
                image = lib.getBuildingImage(building);
            }
        }

        add(new JLabel(longName), "span, align center, wrap 30");
        add(new JLabel(new ImageIcon(image)));
        add(new UnitLabel(getFreeColClient(), unit, false, false), "wrap");
        add(new JLabel(shortName));
        add(new JLabel(ModifierFormat.format(result)));

        Collections.sort(modifiers);
        output(modifiers, unitType);

        result = wl.getPotentialProduction(goodsType, unitType);
        if (result < 0.0f) {
            add(new JLabel(Messages.message("model.source.zeroThreshold.name")),
                           "newline");
            add(new JLabel(ModifierFormat.format(-result)), "wrap 30");
            result = 0.0f;
        }

        Font bigFont = getFont().deriveFont(Font.BOLD, 16);
        JLabel finalLabel
            = new JLabel(Messages.message("model.source.finalResult.name"));
        finalLabel.setFont(bigFont);
        add(finalLabel, "newline");

        JLabel finalResult = new JLabel(ModifierFormat.format(result));
        finalResult.setFont(bigFont);
        finalResult.setBorder(border);
        add(finalResult, "wrap 30");

        Collections.sort(moreModifiers);
        output(moreModifiers, unitType);

        add(okButton, "newline, span, tag ok");
        setSize(getPreferredSize());
    }

    private void output(List<Modifier> modifiers, UnitType unitType) {
        for (Modifier m : modifiers) {
            JLabel[] mLabels
                = ModifierFormat.getModifierLabels(m, unitType, turn);
            for (int i = 0; i < mLabels.length; i++) {
                if (mLabels[i] == null) continue;
                if (i == 0) {
                    add(mLabels[i], "newline");
                } else {
                    add(mLabels[i]);
                }
            }
        }
    }
}
