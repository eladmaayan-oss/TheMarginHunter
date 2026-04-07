const {setGlobalOptions} = require("firebase-functions");
const {onDocumentWritten} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");

initializeApp();

setGlobalOptions({maxInstances: 10});

exports.calculateStockValue = onDocumentWritten("stocks/{ticker}", (event) => {
  if (!event.data.after.exists) return null;

  const before = event.data.before.data() || {};
  const after = event.data.after.data();

  // מניעת לולאה אינסופית: מחשבים רק אם הנתונים הפיננסיים השתנו
  const hasChanged = before.eps !== after.eps ||
                     before.growthRate !== after.growthRate;

  if (!hasChanged && after.intrinsicValue) {
    return null;
  }

  const eps = after.eps || 0;
  const growthRate = after.growthRate || 0;
  const currentYield = after.currentYield || 4.4;

  if (eps === 0) return null;

  // נוסחת בנג'מין גרהם
  const intrinsicValue = (eps * (8.5 + 2 * growthRate) * 4.4) / currentYield;

  console.log(`Clclting value for ${event.params.ticker}: ${intrinsicValue}`);

  return event.data.after.ref.set({
    intrinsicValue: intrinsicValue,
    lastCalculated: Date.now(), // שינוי מ-toISOString() ל-Date.now()
  }, {merge: true});
});
