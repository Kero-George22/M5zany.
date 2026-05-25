import React, { useState, useRef } from 'react';
import { BrowserMultiFormatReader, DecodeHintType, BarcodeFormat } from '@zxing/library';
import { 
  ShoppingCart, 
  Package, 
  QrCode, 
  Search, 
  Trash2, 
  CheckCircle2, 
  AlertCircle, 
  X,
  Camera,
  Plus,
  Upload
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

// Mock products based on the real-world data from the provided images
const MOCK_PRODUCTS = [
  { id: 1, name: "Beyti Full Cream Milk 1L", barcode: "6223001010012", category: "Dairy & Eggs", price: 38.0, stock: 85, expiry: "2026-12-31", location: "A1-R1" },
  { id: 2, name: "Nescafe Classic 1.8g Sachet", barcode: "6221007051114", category: "Beverages", price: 5.5, stock: 500, expiry: "2027-11-20", location: "B2-R5" },
  { id: 3, name: "Heinz Barbecue Sauce 125g", barcode: "6221033001282", category: "Canned & Packaged", price: 18.0, stock: 42, expiry: "2027-01-19", location: "E3-R2" },
  { id: 4, name: "Cocoa Biscuits w/ Marshmallow (EXPIRED)", barcode: "6223005593467", category: "Snacks", price: 7.0, stock: 120, expiry: "2024-10-15", location: "D1-R4" },
  { id: 5, name: "Pepsi 330ml Can (EXPIRED)", barcode: "6221007039037", category: "Beverages", price: 10.0, stock: 200, expiry: "2023-11-15", location: "C2-R1" },
  { id: 6, name: "Almarai Milk 1L", barcode: "6223001874294", category: "Dairy & Eggs", price: 35.0, stock: 12, expiry: "2026-10-10", location: "C1-R2" },
  { id: 9, name: "Baraka Water 1.5L", barcode: "6221040008001", category: "Beverages", price: 5.5, stock: 120, expiry: "2027-01-10", location: "C1-R1" },
  { id: 19, name: "Lays Classic 80g", barcode: "6221050018001", category: "Snacks", price: 13.5, stock: 15, expiry: "2026-06-20", location: "D4-R2" },
  { id: 20, name: "Heinz Tomato Ketchup 300g", barcode: "6221024999925", category: "Canned & Packaged", price: 25.0, stock: 68, expiry: "2027-04-12", location: "E3-R1" },
  { id: 21, name: "Almarai Fresh Milk 1L", barcode: "6281007001402", category: "Dairy & Eggs", price: 42.0, stock: 15, expiry: "2026-11-20", location: "A1-R2" },
  { id: 22, name: "Brimo Strawberry Jam 340g", barcode: "6223000717431", category: "Canned & Packaged", price: 24.5, stock: 30, expiry: "2027-08-15", location: "E2-R3" },
  { id: 23, name: "Dreem Vanilla Cake Mix 400g", barcode: "6223001089186", category: "Canned & Packaged", price: 22.0, stock: 150, expiry: "2026-12-10", location: "D2-R1" },
  { id: 24, name: "El Maleka Vermicelli 1kg", barcode: "6223001510451", category: "Canned & Packaged", price: 18.0, stock: 45, expiry: "2026-09-15", location: "F1-R2" },
  { id: 25, name: "Al-Yemeni Cafe Plain Coffee 100g", barcode: "6223000111284", category: "Beverages", price: 42.0, stock: 30, expiry: "2027-05-14", location: "B1-R3" },
];

export default function App() {
  // Initialize reader once for high-speed retail scanning at the top
  const reader = useRef(new BrowserMultiFormatReader(
    new Map<DecodeHintType, any>([
      [DecodeHintType.POSSIBLE_FORMATS, [
        BarcodeFormat.EAN_13,
        BarcodeFormat.EAN_8,
        BarcodeFormat.UPC_A,
        BarcodeFormat.UPC_E,
        BarcodeFormat.CODE_128,
        BarcodeFormat.QR_CODE,
        BarcodeFormat.ITF,
        BarcodeFormat.CODE_39,
        BarcodeFormat.DATA_MATRIX,
        BarcodeFormat.RSS_14,
        BarcodeFormat.RSS_EXPANDED,
        BarcodeFormat.CODE_93
      ]],
      [DecodeHintType.TRY_HARDER, true],
      [DecodeHintType.CHARACTER_SET, 'utf-8'],
      [DecodeHintType.ASSUME_GS1, true]
    ])
  ));

  const [products, setProducts] = useState(MOCK_PRODUCTS);
  const [userRole, setUserRole] = useState<'admin' | 'cashier' | null>(null);
  const [usernameInput, setUsernameInput] = useState("");
  const [passwordInput, setPasswordInput] = useState("");
  const [loginError, setLoginError] = useState("");
  const [cart, setCart] = useState<any[]>([]);
  const [isScanning, setIsScanning] = useState(false);
  const [isManualEntryOpen, setIsManualEntryOpen] = useState(false);
  const [scanFlash, setScanFlash] = useState(false);
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncStatus, setSyncStatus] = useState<'idle' | 'fetching' | 'success' | 'not_found' | 'error'>('idle');
  const [activeScanCode, setActiveScanCode] = useState<string | null>(null);
  const [batchAmount, setBatchAmount] = useState(10);
  const [cameraIndex, setCameraIndex] = useState(0);
  const [activeCameraLabel, setActiveCameraLabel] = useState<string>("Initializing...");
  const [lastScanned, setLastScanned] = useState<any>(null);
  // Initialize camera selection
  React.useEffect(() => {
    const initCamera = async () => {
      try {
        const devices = await reader.current.listVideoInputDevices();
        const backIdx = devices.findIndex(d => /back|rear|environment/i.test(d.label));
        if (backIdx !== -1) {
          setCameraIndex(backIdx);
          setActiveCameraLabel(devices[backIdx].label);
        } else if (devices.length > 0) {
          setActiveCameraLabel(devices[0].label);
        }
      } catch (e) {
        console.error("Camera Init Error:", e);
      }
    };
    initCamera();
  }, []);

  const [searchQuery, setSearchQuery] = useState("");
  const [newProduct, setNewProduct] = useState({
    name: "",
    barcode: "",
    category: "General",
    price: "",
    stock: "",
    expiry: "",
    location: ""
  });
  const [formError, setFormError] = useState("");
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsSyncing(true);
    setSyncStatus('fetching');
    setFormError("");

    try {
      const imageUrl = URL.createObjectURL(file);
      const result = await reader.current.decodeFromImageUrl(imageUrl);
      const code = result.getText().trim();
      
      console.log("File Upload: Decoded ->", code);
      setActiveScanCode(code);
      
      // Check local first to avoid unnecessary syncs
      const localProduct = productsRef.current.find(p => p.barcode === code);
      if (localProduct) {
        setLastScanned({ ...localProduct, status: 'found' });
        setSyncStatus('success');
      } else {
        await syncExternalProduct(code);
      }
      
      URL.revokeObjectURL(imageUrl);
    } catch (err) {
      console.error("Image Decode Error:", err);
      setSyncStatus('error');
      setLastScanned({ barcode: "IMAGE_INPUT", status: 'not_found' });
      setFormError("OPTIC_REJECTION: No valid barcode signature detected in the image. Ensure high contrast and zero blur.");
      setIsManualEntryOpen(true);
    } finally {
      setIsSyncing(false);
      // Reset input so the same file can be uploaded again if needed
      if (e.target) e.target.value = '';
    }
  };

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError("");
    
    const user = usernameInput.toLowerCase().trim();
    if (user === "admin" && passwordInput === "admin123") {
      setUserRole('admin');
    } else if (user === "cashier" && passwordInput === "cash123") {
      setUserRole('cashier');
    } else {
      setLoginError("ACCESS_DENIED: Invalid username or password.");
    }
  };

  const logout = () => {
    setUserRole(null);
    setUsernameInput("");
    setPasswordInput("");
    setCart([]);
    stopScanner();
  };
  const videoRef = useRef<HTMLVideoElement>(null);
  const productsRef = useRef(products);
  const lastProcessedCode = useRef<string | null>(null);
  const cooldownTimer = useRef<NodeJS.Timeout | null>(null);
  
  // Sync products ref for the scanner callback
  React.useEffect(() => {
    productsRef.current = products;
  }, [products]);

  // Flash effect on successful scan
  React.useEffect(() => {
    if (lastScanned) {
      setScanFlash(true);
      const timer = setTimeout(() => setScanFlash(false), 200);
      return () => clearTimeout(timer);
    }
  }, [lastScanned]);

  const filteredProducts = products.filter(p => 
    p.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
    p.barcode.includes(searchQuery)
  );

  const handleAddProduct = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError("");

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const expiryDate = new Date(newProduct.expiry);

    // Barcode Validation: EAN-8, UPC-A (12), EAN-13, ITF-14 (14)
    const isRetailBarcode = /^\d{8}$|^\d{12}$|^\d{13}$|^\d{14}$/.test(newProduct.barcode);
    const isAlphanumericCode = /^[a-zA-Z0-9]{4,20}$/.test(newProduct.barcode);

    if (!isRetailBarcode && !isAlphanumericCode) {
      setFormError("BARCODE REJECTED: Must be a standard retail code (8, 12, 13, 14 digits) or an alphanumeric ID (4-20 chars).");
      return;
    }

    if (expiryDate < today) {
      setFormError("CRITICAL: Expiry date cannot be in the past for new inventory.");
      return;
    }

    const product = {
      ...newProduct,
      id: products.length + 1,
      price: parseFloat(newProduct.price),
      stock: parseInt(newProduct.stock),
    };

    setProducts(prev => [product, ...prev]);
    setIsManualEntryOpen(false);
    setNewProduct({
      name: "",
      barcode: "",
      category: "General",
      price: "",
      stock: "",
      expiry: "",
      location: ""
    });
  };

  const addToCart = (product: any) => {
    setCart(prev => {
      const existing = prev.find(item => item.id === product.id);
      if (existing) {
        return prev.map(item => item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item);
      }
      return [...prev, { ...product, quantity: 1 }];
    });
  };

  const removeFromCart = (id: number) => {
    setCart(prev => prev.filter(item => item.id !== id));
  };

  const total = cart.reduce((acc, item) => acc + item.price * item.quantity, 0);

  const restockProduct = (id: number, amount: number) => {
    setProducts(prev => prev.map(p => p.id === id ? { ...p, stock: Math.max(0, p.stock + amount) } : p));
    // If it's the last scanned item, update that view too
    setLastScanned(prev => (prev && prev.id === id) ? { ...prev, stock: Math.max(0, prev.stock + amount) } : prev);
  };

  const deleteProduct = (id: number) => {
    setProducts(prev => prev.filter(p => p.id !== id));
    setCart(prev => prev.filter(p => p.id !== id));
    if (lastScanned && lastScanned.id === id) setLastScanned(null);
  };

  const syncExternalProduct = async (barcode: string) => {
    setIsSyncing(true);
    setSyncStatus('fetching');
    try {
      // Calling our local Express backend proxy to avoid CORS and handle telemetry
      const response = await fetch(`/api/products/${barcode}`);
      const data = await response.json();

      if (data.status === 1) {
        const p = data.product;
        const syncedProduct = {
          barcode: barcode,
          name: p.product_name || p.generic_name || "New External Item",
          category: p.categories?.split(',')[0] || "Uncategorized",
          price: 15.0, // Default estimate for registration
          stock: 50,
          expiry: new Date(Date.now() + 180 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
          location: "SCANNER_ENTRY",
          brand: p.brands || "Generic",
          imageUrl: p.image_front_small_url
        };
        setLastScanned({ ...syncedProduct, status: 'synced_found' });
        setSyncStatus('success');
      } else {
        setLastScanned({ barcode: barcode, status: 'not_found' });
        setSyncStatus('not_found');
      }
    } catch (error) {
      console.error("Global Sync Error:", error);
      setLastScanned({ barcode: barcode, status: 'not_found' });
      setSyncStatus('error');
    } finally {
      setIsSyncing(false);
    }
  };

  const startScanner = () => {
    setIsScanning(true);
    setLastScanned(null);
  };

  const stopScanner = () => {
    reader.current.reset();
    setIsScanning(false);
    setLastScanned(null);
    setSyncStatus('idle');
    setActiveScanCode(null);
  };

  const [isCapturing, setIsCapturing] = useState(false);

  // Precision Manual Snapshot Scan (Force decode current frame)
  const captureAndScan = async () => {
    if (!videoRef.current || !isScanning) return;
    setIsCapturing(true);

    try {
      const video = videoRef.current;
      const canvas = document.createElement('canvas');
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        const dataUrl = canvas.toDataURL('image/jpeg', 0.95);
        
        try {
          const result = await reader.current.decodeFromImageUrl(dataUrl);
          if (result) {
            console.log("Scanner: Snap Scan SUCCESS ->", result.getText());
            processFoundCode(result.getText().trim());
          }
        } catch (e) {
          console.warn("Snap Scan: No code found in frame.");
        }
      }
    } catch (err) {
      console.warn("Manual Capture Error:", err);
    } finally {
      setTimeout(() => setIsCapturing(false), 400);
    }
  };

  const processFoundCode = (code: string) => {
    if (code === lastProcessedCode.current) return;
    
    // Immediate Visual Feedback
    setScanFlash(true);
    setTimeout(() => setScanFlash(false), 200);
    
    console.log("Scanner Logic: SUCCESS ->", code);
    lastProcessedCode.current = code;
    setActiveScanCode(code);
    
    const product = productsRef.current.find(p => p.barcode === code);
    if (product) {
      setLastScanned({ ...product, status: 'found' });
      if (userRole === 'cashier') {
        // Reducer functionality for cashier: Instant stock deduction
        if (product.stock > 0) {
          setProducts(prev => prev.map(p => p.id === product.id ? { ...p, stock: Math.max(0, p.stock - 1) } : p));
          // Add to a "Session Log" for the cashier specifically
          setCart(prev => {
            const existing = prev.find(item => item.id === product.id);
            if (existing) {
              return prev.map(item => item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item);
            }
            return [...prev, { ...product, quantity: 1 }];
          });
        }
      }
    } else {
      syncExternalProduct(code);
    }

    // Faster cooldown for snappy scanning
    if (cooldownTimer.current) clearTimeout(cooldownTimer.current);
    cooldownTimer.current = setTimeout(() => { 
      lastProcessedCode.current = null; 
      setActiveScanCode(null);
    }, 800);
  };

  // Core Real-Time Scanner Effect
  React.useEffect(() => {
    let isMounted = true;
    if (isScanning && videoRef.current) {
      const startCamera = async () => {
        try {
          const videoInputDevices = await reader.current.listVideoInputDevices();
          if (!isMounted || videoInputDevices.length === 0) return;

          const selectedDevice = videoInputDevices[cameraIndex % videoInputDevices.length];
          if (!selectedDevice) return;

          const selectedDeviceId = selectedDevice.deviceId;
          setActiveCameraLabel(selectedDevice.label);

          // Advanced constraints for sharp retail codes
          const constraints: MediaStreamConstraints = {
            video: {
              deviceId: selectedDeviceId ? { ideal: selectedDeviceId } : undefined,
              width: { ideal: 1920, min: 1280 },
              height: { ideal: 1080, min: 720 },
              facingMode: 'environment',
              aspectRatio: { ideal: 1.7777777778 },
              frameRate: { ideal: 60, min: 30 },
              ...({ focusMode: { ideal: 'continuous' } } as any)
            }
          };

          await reader.current.decodeFromConstraints(constraints, videoRef.current, (result, error) => {
            if (!isMounted) return;
            if (result) {
              const code = result.getText().trim();
              processFoundCode(code);
            }
          });
        } catch (err) {
          console.error("Scanner Error:", err);
          // Fallback to basic method if constraints fail
          try {
            await reader.current.decodeFromVideoDevice(undefined, videoRef.current!, (res) => {
              if (res) processFoundCode(res.getText().trim());
            });
          } catch (e2) {
            if (isMounted) setIsScanning(false);
          }
        }
      };
      startCamera();
    }
    return () => {
      isMounted = false;
      reader.current.reset();
    };
  }, [isScanning, cameraIndex]);

  return (
    <div className="flex flex-col h-screen w-full bg-slate-100 overflow-hidden font-sans">
      <AnimatePresence>
        {!userRole && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[100] bg-slate-950 flex items-center justify-center p-6"
          >
            <div className="absolute inset-0 opacity-20 pointer-events-none">
              <div className="absolute top-0 left-0 w-full h-full bg-[radial-gradient(circle_at_center,_var(--tw-gradient-stops))] from-blue-900 via-transparent to-transparent"></div>
            </div>

            <motion.div 
              initial={{ y: 20, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              className="w-full max-w-md bg-white rounded-3xl overflow-hidden shadow-[0_0_50px_rgba(37,99,235,0.2)] border border-slate-800/10 p-10 relative z-10"
            >
              <div className="flex flex-col items-center mb-8 text-center">
                <div className="w-16 h-16 bg-blue-600 rounded-2xl flex items-center justify-center mb-4 shadow-xl shadow-blue-600/30">
                  <QrCode size={32} className="text-white" />
                </div>
                <h2 className="text-2xl font-black text-slate-900 tracking-tight">Repo Access Control</h2>
                <p className="text-xs font-bold text-slate-400 uppercase tracking-widest mt-2">Enter Authority Key to Open Terminal</p>
              </div>

              <form onSubmit={handleLogin} className="space-y-4">
                <div>
                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-[0.2em] mb-2 block">System Identity</label>
                  <input 
                    type="text" 
                    placeholder="Username"
                    value={usernameInput}
                    onChange={(e) => setUsernameInput(e.target.value)}
                    className="w-full px-5 py-3 bg-slate-50 border border-slate-100 rounded-xl text-sm font-bold tracking-wider focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 outline-none transition-all text-center"
                    autoFocus
                  />
                </div>

                <div>
                  <label className="text-[10px] font-black text-slate-400 uppercase tracking-[0.2em] mb-2 block">Authority Key</label>
                  <input 
                    type="password" 
                    placeholder="••••••••"
                    value={passwordInput}
                    onChange={(e) => setPasswordInput(e.target.value)}
                    className="w-full px-5 py-3 bg-slate-50 border border-slate-100 rounded-xl text-lg font-mono tracking-widest focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 outline-none transition-all text-center"
                  />
                </div>

                {loginError && (
                  <p className="text-[10px] font-black text-red-600 uppercase tracking-widest text-center">{loginError}</p>
                )}

                <button 
                  type="submit"
                  className="w-full bg-slate-900 text-white py-4 rounded-2xl font-black uppercase tracking-[0.3em] text-[10px] hover:bg-black transition-all active:scale-95 shadow-2xl mt-4"
                >
                  Confirm Credentials
                </button>
              </form>

              <div className="mt-8 pt-8 border-t border-slate-100 flex justify-center gap-6">
                 <div className="text-center">
                   <p className="text-[8px] font-black text-slate-300 uppercase tracking-widest">Administrator</p>
                   <p className="text-[9px] font-mono text-slate-400 mt-1">admin / admin123</p>
                 </div>
                 <div className="text-center border-l border-slate-100 pl-6">
                   <p className="text-[8px] font-black text-slate-300 uppercase tracking-widest">Cashier Point</p>
                   <p className="text-[9px] font-mono text-slate-400 mt-1">cashier / cash123</p>
                 </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Clean Modern Header */}
      <header className="h-16 bg-slate-900 text-white flex items-center justify-between px-8 border-b border-slate-800 shrink-0">
        <div className="flex items-center space-x-4">
          <div className="bg-blue-600 p-2 rounded-lg shadow-lg">
            <QrCode className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-tight">Repo Logic <span className="text-slate-400 font-normal">| Inventory Control</span></h1>
          </div>
        </div>
        
        <div className="flex items-center space-x-6">
          <div className="h-8 w-px bg-slate-700"></div>
          <div className="text-right flex items-center gap-4">
            <div>
              <p className="text-[10px] font-black text-blue-500 uppercase tracking-widest leading-none mb-1">{userRole === 'admin' ? 'Manager Access' : 'Cashier Terminal'}</p>
              <p className="text-sm font-medium leading-tight text-white">{userRole === 'admin' ? 'Store Administrator' : 'Operator 01'}</p>
            </div>
            <button 
              onClick={logout}
              className="text-[10px] bg-red-600/20 text-red-500 border border-red-500/30 px-3 py-1.5 rounded-lg hover:bg-red-600 hover:text-white transition-all uppercase font-black tracking-widest"
            >
              Sign Out
            </button>
          </div>
        </div>
      </header>

      {/* Main Workspace */}
      <main className="flex flex-1 overflow-hidden">
        
        {/* Left: Product Selection & Inventory */}
        <section className="flex-1 flex flex-col bg-slate-50 overflow-hidden border-r border-slate-300 relative">
          <div className="p-6 border-b border-slate-200 bg-white flex items-center justify-between">
            <h2 className="text-sm font-bold text-slate-500 uppercase tracking-widest flex items-center gap-2">
              <Package size={16} />
              Stock Repository
            </h2>
            <div className="flex items-center gap-3">
              <div className="flex items-center bg-slate-100 border border-slate-200 rounded-lg px-2 gap-2">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest pl-1">Batch:</span>
                <select 
                  value={batchAmount}
                  onChange={(e) => setBatchAmount(parseInt(e.target.value))}
                  className="bg-transparent text-xs font-bold text-slate-700 outline-none py-1.5 cursor-pointer"
                >
                  <option value="1">1</option>
                  <option value="5">5</option>
                  <option value="10">10</option>
                  <option value="25">25</option>
                  <option value="50">50</option>
                  <option value="100">100</option>
                </select>
              </div>

              <div className="relative">
                <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input 
                  type="text" 
                  placeholder="Filter stock..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-9 pr-3 py-1.5 bg-slate-100 border border-slate-200 rounded-lg text-xs outline-none focus:bg-white focus:border-blue-500 transition-all w-48"
                />
              </div>

              <input 
                type="file" 
                ref={fileInputRef}
                onChange={handleFileUpload}
                accept="image/*"
                className="hidden"
              />
              
              <button 
                onClick={() => fileInputRef.current?.click()}
                className="bg-white border border-slate-200 text-slate-700 px-4 py-1.5 rounded-lg text-xs font-bold flex items-center gap-2 transition-all hover:bg-slate-100 active:scale-95"
              >
                <Upload size={14} />
                Upload Image
              </button>

              <button 
                onClick={startScanner}
                disabled={isScanning}
                className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-1.5 rounded-lg text-xs font-bold flex items-center gap-2 transition-all active:scale-95 disabled:opacity-50"
              >
                <Camera size={14} />
                Launch CV
              </button>
              <button 
                onClick={() => setIsManualEntryOpen(true)}
                className="bg-slate-900 border border-slate-700 hover:bg-slate-800 text-white px-4 py-1.5 rounded-lg text-xs font-bold flex items-center gap-2 transition-all active:scale-95"
              >
                <Package size={14} />
                Manual Input
              </button>
            </div>
          </div>

          <div className="flex-1 overflow-y-auto p-6 custom-scrollbar">
            <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
              {filteredProducts.map(product => {
                const isExpired = new Date(product.expiry) < new Date();
                return (
                  <motion.div 
                    layout
                    key={product.id}
                    className={`group p-4 bg-white rounded-xl border border-slate-200 hover:border-blue-400 hover:shadow-xl hover:shadow-blue-500/5 transition-all relative overflow-hidden ${
                      isExpired ? 'ring-1 ring-red-100' : ''
                    }`}
                  >
                    {isExpired && (
                      <div className="absolute top-0 right-0 bg-red-600 text-white text-[8px] px-2 py-1 font-black uppercase tracking-widest rounded-bl-lg">
                        Expired Target
                      </div>
                    )}
                    <div className="flex justify-between items-start mb-3">
                      <span className="text-[9px] font-black tracking-widest text-blue-600 uppercase bg-blue-50 px-2 py-0.5 rounded">
                        {product.category}
                      </span>
                      <code className="text-[10px] text-slate-400 font-mono">ID:{product.id}</code>
                    </div>
                    <h3 className="font-bold text-slate-900 group-hover:text-blue-600 transition-colors">{product.name}</h3>
                      <div className="mt-4 flex items-end justify-between">
                        <div>
                          <p className="text-xl font-black text-slate-900 tracking-tighter">
                            {product.price.toFixed(2)} 
                            <span className="text-[10px] font-normal text-slate-400 ml-1">EGP/UNIT</span>
                          </p>
                          <p className={`text-[10px] font-bold mt-1 ${product.stock < 20 ? 'text-red-500' : 'text-slate-500'}`}>
                            STOCK: {product.stock} UNITS
                          </p>
                        </div>
                        {userRole === 'admin' && (
                          <div className="flex gap-1">
                            <button 
                               onClick={() => restockProduct(product.id, batchAmount)}
                               className="w-10 h-8 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-all font-black text-xs flex items-center justify-center"
                             >+</button>
                             <button 
                               onClick={() => restockProduct(product.id, -batchAmount)}
                               className="w-10 h-8 bg-slate-900 text-white rounded-lg hover:bg-black transition-all font-black text-xs flex items-center justify-center"
                             >-</button>
                             <button 
                               onClick={() => deleteProduct(product.id)}
                               className="p-2 bg-slate-100 text-slate-400 rounded-lg hover:bg-red-50 hover:text-red-600 transition-all ml-1"
                             ><Trash2 size={14}/></button>
                          </div>
                        )}
                        {userRole === 'cashier' && (
                          <button 
                            onClick={() => {
                              if (product.stock > 0) {
                                setProducts(prev => prev.map(p => p.id === product.id ? { ...p, stock: Math.max(0, p.stock - 1) } : p));
                                addToCart(product);
                              }
                            }}
                            className="p-2 bg-slate-900 text-white rounded-lg hover:bg-blue-600 transition-all shadow-lg active:scale-90"
                            title="Register Sale"
                          >
                            <ShoppingCart size={16} />
                          </button>
                        )}
                      </div>
                  </motion.div>
                );
              })}
            </div>
          </div>

          {/* Integrated Scanner Overlay */}
          <AnimatePresence>
            {isScanning && (
              <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="absolute inset-0 z-20 bg-black flex flex-col items-center justify-center"
              >
                <div className="relative w-full h-full flex items-center justify-center">
                  <video 
                    ref={videoRef} 
                    autoPlay 
                    muted 
                    playsInline 
                    className="w-full h-full object-contain bg-slate-950" 
                  />
                  
                  <div className={`absolute inset-0 z-10 bg-white pointer-events-none transition-opacity duration-200 ${scanFlash ? 'opacity-40' : 'opacity-0'}`}></div>
                  
                  {/* Scanner UI Sights */}
                  <div className="absolute w-[480px] h-[360px] max-w-[80%] max-h-[80%] border-2 border-dashed border-slate-500/30 rounded-2xl flex flex-col items-center justify-center pointer-events-none">
                    <div className="absolute top-0 left-0 w-12 h-12 border-t-4 border-l-4 border-indigo-500/80 -mt-1 -ml-1"></div>
                    <div className="absolute top-0 right-0 w-12 h-12 border-t-4 border-r-4 border-indigo-500/80 -mt-1 -mr-1"></div>
                    <div className="absolute bottom-0 left-0 w-12 h-12 border-b-4 border-l-4 border-indigo-500/80 -mb-1 -ml-1"></div>
                    <div className="absolute bottom-0 right-0 w-12 h-12 border-b-4 border-r-4 border-indigo-500/80 -mb-1 -mr-1"></div>
                    
                    <div className="absolute top-0 left-0 right-0 h-0.5 bg-indigo-500/50 animate-scan-fast shadow-[0_0_15px_rgba(99,102,241,0.5)]"></div>
                    
                    <div className="absolute top-0 left-0 p-4">
                      <div className={`text-white text-[8px] font-black tracking-[0.3em] uppercase px-3 py-1.5 rounded shadow-2xl flex items-center gap-2 transition-colors ${isSyncing ? 'bg-orange-600' : 'bg-green-600'}`}>
                        <div className={`w-1.5 h-1.5 bg-white rounded-full ${isSyncing ? 'animate-spin' : 'animate-pulse'}`}></div>
                        {isSyncing ? 'Synchronizing...' : 'Optic Link: Stable'}
                      </div>
                    </div>

                    <div className="absolute top-0 right-0 p-4 flex flex-col items-end gap-2">
                       <div className={`text-white text-[8px] font-black tracking-[0.3em] uppercase ${isSyncing ? 'bg-orange-500 animate-pulse ring-4 ring-orange-500/30' : 'bg-blue-600'} px-3 py-1.5 rounded shadow-2xl transition-all duration-300`}>
                        {isSyncing ? 'Sync Active' : 'Auto-Decode: ON'}
                      </div>
                    </div>
                  </div>

                  <div className="absolute bottom-12 flex items-center gap-4">
                    <button 
                      onClick={captureAndScan}
                      disabled={isCapturing}
                      className={`bg-blue-600 border border-blue-400 text-white px-8 py-4 rounded-xl text-xs font-black uppercase tracking-[0.2em] shadow-2xl transition-all flex items-center gap-2 ${isCapturing ? 'scale-95 opacity-80' : 'hover:bg-blue-500 hover:scale-105 active:scale-95'}`}
                    >
                      {isCapturing ? (
                        <>
                          <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                          Processing...
                        </>
                      ) : (
                        <>
                          <Camera size={18} />
                          Capture Frame
                        </>
                      )}
                    </button>
                    <button 
                      onClick={() => {
                        reader.current.reset();
                        setIsScanning(false);
                        setCameraIndex(prev => prev + 1);
                        setTimeout(() => setIsScanning(true), 200);
                      }}
                      className="bg-blue-600/20 backdrop-blur-md border border-blue-500/30 text-white px-6 py-3 rounded-xl text-[10px] font-black uppercase tracking-[0.2em] hover:bg-blue-600/40 transition-all font-bold group"
                    >
                      <Camera size={14} className="group-hover:rotate-180 transition-transform duration-500" />
                      Switch Camera ({activeCameraLabel})
                    </button>
                    <button 
                      onClick={stopScanner}
                      className="bg-red-600/20 backdrop-blur-md border border-red-500/30 text-white px-8 py-4 rounded-xl text-xs font-black uppercase tracking-[0.2em] hover:bg-red-600/40 transition-all font-bold"
                    >
                      Close Scanner
                    </button>
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </section>

        {/* Right Sidebar: Details & Checkout */}
        <aside className="w-[360px] bg-white border-l border-slate-300 flex flex-col shrink-0">
          <div className="p-6 border-b border-slate-100 bg-slate-50/50 shrink-0">
            <h3 className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-4">Real-Time Detection</h3>
            
            <AnimatePresence mode="wait">
              {isSyncing ? (
                <motion.div 
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="h-32 flex flex-col items-center justify-center border-2 border-blue-100 bg-blue-50/30 rounded-xl gap-3"
                >
                  <div className="w-6 h-6 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
                  <p className="text-[10px] font-black text-blue-600 uppercase tracking-widest animate-pulse">Syncing Global Identity...</p>
                </motion.div>
              ) : lastScanned ? (
                <motion.div 
                  initial={{ x: 20, opacity: 0 }}
                  animate={{ x: 0, opacity: 1 }}
                  className={`p-4 rounded-xl border-2 ${
                    lastScanned.status === 'found' || lastScanned.status === 'synced_found'
                      ? (new Date(lastScanned.expiry) < new Date() ? 'bg-red-50 border-red-200' : 'bg-blue-50 border-blue-200 shadow-lg shadow-blue-500/5')
                      : 'bg-slate-50 border-slate-200'
                  }`}
                >
                  <div className="flex justify-between items-start mb-3">
                    <div>
                      <p className={`text-[9px] font-black uppercase tracking-widest mb-1 ${
                        lastScanned.status === 'found' 
                          ? (new Date(lastScanned.expiry) < new Date() ? 'text-red-600' : 'text-blue-600')
                          : 'text-slate-400'
                      }`}>
                        {lastScanned.status === 'found' 
                          ? (new Date(lastScanned.expiry) < new Date() ? 'Status: Expired' : 'Status: Authenticated')
                          : 'Wait Pattern'}
                      </p>
                      <h4 className="font-black text-slate-900 text-lg leading-tight">
                        {lastScanned.status === 'found' ? lastScanned.name : 'Unknown Object'}
                      </h4>
                    </div>
                    {lastScanned.status === 'found' && (
                       <span className={`px-2 py-0.5 text-[8px] font-black uppercase rounded ${
                         new Date(lastScanned.expiry) < new Date() ? 'bg-red-600 text-white' : 'bg-blue-600 text-white'
                       }`}>
                         {new Date(lastScanned.expiry) < new Date() ? 'Deny' : 'Pass'}
                       </span>
                    )}
                  </div>

                  {lastScanned.status === 'found' ? (
                    <div className="space-y-4">
                      <div className="grid grid-cols-2 gap-3">
                        <div className="bg-white/50 p-2 rounded-lg border border-slate-100">
                           <p className="text-[8px] font-bold text-slate-400 uppercase tracking-widest">Inventory Location</p>
                           <p className="text-sm font-black text-slate-700">{lastScanned.location || "N/A"}</p>
                        </div>
                        <div className="bg-white/50 p-2 rounded-lg border border-slate-100">
                           <p className="text-[8px] font-bold text-slate-400 uppercase tracking-widest">Unit Price</p>
                           <p className="text-sm font-black text-slate-700">{lastScanned.price.toFixed(2)} EGP</p>
                        </div>
                        <div className="bg-white/50 p-2 rounded-lg border border-slate-100 relative group">
                           <p className="text-[8px] font-bold text-slate-400 uppercase tracking-widest">Available Stock</p>
                           <p className={`text-sm font-black ${lastScanned.stock < 10 ? 'text-red-600' : 'text-slate-700'}`}>{lastScanned.stock} units</p>
                           <div className="absolute top-1 right-1 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                             <button 
                               onClick={() => restockProduct(lastScanned.id, batchAmount)}
                               className="w-6 h-6 bg-blue-600 text-white rounded text-[10px] font-bold flex items-center justify-center"
                             >+</button>
                             <button 
                               onClick={() => restockProduct(lastScanned.id, -batchAmount)}
                               className="w-6 h-6 bg-slate-900 text-white rounded text-[10px] font-bold flex items-center justify-center"
                             >-</button>
                           </div>
                        </div>
                        <div className="bg-white/50 p-2 rounded-lg border border-slate-100">
                           <p className="text-[8px] font-bold text-slate-400 uppercase tracking-widest">Shelf Expiry</p>
                           <p className="text-sm font-black text-slate-700">{lastScanned.expiry}</p>
                        </div>
                      </div>

                      <div className="p-3 bg-slate-900/5 rounded-xl border border-slate-200/50 space-y-3">
                        <div className="grid grid-cols-2 gap-2">
                          <button 
                            onClick={() => syncExternalProduct(lastScanned.barcode)}
                            disabled={isSyncing}
                            className={`flex-1 border py-2 rounded-lg text-[9px] font-bold uppercase tracking-tighter transition-all flex items-center justify-center gap-1 ${
                              isSyncing 
                                ? 'bg-slate-50 text-slate-300 border-slate-100' 
                                : 'bg-white border-indigo-200 text-indigo-600 hover:bg-indigo-50 shadow-sm'
                            }`}
                          >
                            <QrCode size={10} className={isSyncing ? 'animate-spin' : ''} />
                            Refresh Link
                          </button>
                          {userRole === 'cashier' ? (
                            <button 
                              onClick={() => {
                                if (lastScanned.stock > 0) {
                                  setProducts(prev => prev.map(p => p.id === lastScanned.id ? { ...p, stock: Math.max(0, p.stock - 1) } : p));
                                  addToCart(lastScanned);
                                }
                              }}
                              disabled={new Date(lastScanned.expiry) < new Date() || lastScanned.stock <= 0}
                              className="bg-blue-600 text-white py-2 rounded-lg text-[9px] font-bold uppercase tracking-tighter hover:bg-blue-700 transition-colors shadow-lg shadow-blue-500/20 disabled:opacity-30 flex items-center justify-center gap-1"
                            >
                              <ShoppingCart size={10} />
                              Log Sale
                            </button>
                          ) : (
                            <div className="flex gap-1">
                              <button 
                                onClick={() => restockProduct(lastScanned.id, batchAmount)}
                                className="flex-1 bg-blue-600 text-white py-2 rounded-lg text-[9px] font-bold uppercase tracking-tighter"
                              >+{batchAmount}</button>
                              <button 
                                onClick={() => restockProduct(lastScanned.id, -batchAmount)}
                                className="flex-1 bg-slate-900 text-white py-2 rounded-lg text-[9px] font-bold uppercase tracking-tighter"
                              >-{batchAmount}</button>
                            </div>
                          )}
                        </div>
                        <button 
                          onClick={() => deleteProduct(lastScanned.id)}
                          className="w-full py-2 bg-white border border-red-100 text-red-500 hover:bg-red-50 transition-colors rounded-lg text-[9px] font-bold uppercase tracking-widest flex items-center justify-center gap-2"
                        >
                          <Trash2 size={10} />
                          Remove Item
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="space-y-4">
                      {lastScanned.status === 'synced_found' ? (
                        <div className="p-4 bg-indigo-50 border-2 border-indigo-200 rounded-xl shadow-inner">
                          <div className="flex items-center gap-3 mb-4">
                            <div className="w-14 h-14 bg-white rounded-lg border border-indigo-100 flex items-center justify-center p-1 overflow-hidden shrink-0">
                              {lastScanned.imageUrl ? (
                                <img src={lastScanned.imageUrl} alt="Product" className="w-full h-full object-contain" />
                              ) : (
                                <QrCode className="text-indigo-300" size={24} />
                              )}
                            </div>
                            <div className="min-w-0">
                              <p className="text-[8px] font-black text-indigo-600 uppercase tracking-[0.2em]">Global Sync Detected</p>
                              <p className="text-sm font-bold text-slate-800 truncate">{lastScanned.name}</p>
                              <p className="text-[10px] text-indigo-500 font-mono truncate">{lastScanned.brand}</p>
                            </div>
                          </div>
                          
                          <div className="flex gap-2">
                             <button 
                               onClick={() => {
                                 const newId = Math.max(...products.map(p => p.id)) + 1;
                                 const addedProduct = { ...lastScanned, id: newId };
                                 setProducts(prev => [...prev, addedProduct]);
                                 setLastScanned({ ...addedProduct, status: 'found' });
                                 if (userRole === 'cashier') {
                                   setProducts(prev => prev.map(p => p.barcode === addedProduct.barcode ? { ...p, stock: Math.max(0, p.stock - 1) } : p));
                                   addToCart(addedProduct);
                                 }
                                 setSyncStatus('idle');
                               }}
                               className="flex-1 bg-indigo-600 text-white py-3 rounded-lg text-[10px] font-black uppercase tracking-widest shadow-lg shadow-indigo-500/20 hover:bg-indigo-700 transition-all flex items-center justify-center gap-2"
                             >
                               <Plus size={14} />
                               Quick Onboard
                             </button>
                             {userRole === 'admin' && (
                               <button 
                                 onClick={() => {
                                   setNewProduct({ 
                                     ...newProduct, 
                                     barcode: lastScanned.barcode,
                                     name: lastScanned.name,
                                     category: lastScanned.category
                                   });
                                   setIsManualEntryOpen(true);
                                 }}
                                 className="p-3 bg-white border border-indigo-200 text-indigo-600 rounded-lg hover:bg-indigo-50 transition-colors"
                                 title="Edit Details"
                               >
                                 <AlertCircle size={16} />
                               </button>
                             )}
                          </div>
                        </div>
                      ) : (
                        <div className="p-4 bg-orange-50 border-2 border-dashed border-orange-200 rounded-xl">
                          <p className="text-[9px] font-black text-orange-600 uppercase tracking-widest mb-1">Unmapped Signal</p>
                          <p className="text-[10px] text-orange-800 font-mono mb-4 break-all">ID: {lastScanned.barcode}</p>
                          
                          <button 
                            onClick={() => {
                              setNewProduct({ ...newProduct, barcode: lastScanned.barcode });
                              setIsManualEntryOpen(true);
                            }}
                            className="w-full bg-orange-600 text-white py-3 rounded-lg text-[10px] font-black uppercase tracking-widest shadow-lg shadow-orange-500/20 hover:bg-orange-700 transition-all flex items-center justify-center gap-2"
                          >
                            <CheckCircle2 size={14} />
                            Manual Register
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </motion.div>
              ) : (
                <div className="h-32 flex flex-col items-center justify-center border-2 border-dashed border-slate-100 rounded-xl gap-2">
                  <QrCode size={24} className="text-slate-100" />
                  <p className="text-[10px] font-bold text-slate-300 uppercase tracking-widest">Waiting for Optic Link...</p>
                </div>
              )}
            </AnimatePresence>
          </div>

          <div className="flex-1 flex flex-col overflow-hidden">
            <div className="p-6 shrink-0 pb-2">
              <h3 className="text-[10px] font-black text-slate-400 uppercase tracking-widest flex items-center justify-between mb-2">
                {userRole === 'cashier' ? 'Scan Output Log' : 'Stock Movement'}
                <span className="px-2 py-0.5 bg-slate-100 text-slate-500 rounded font-mono">{cart.length}</span>
              </h3>
            </div>

            <div className="flex-1 overflow-y-auto px-6 space-y-3 custom-scrollbar pb-4 min-h-[120px]">
              <AnimatePresence mode="popLayout">
                {cart.length === 0 ? (
                  <div className="h-full flex flex-col items-center justify-center text-slate-300 gap-2 grayscale opacity-50">
                    <Package size={32} strokeWidth={1} />
                    <p className="text-[10px] font-bold uppercase tracking-widest">Repository Static</p>
                  </div>
                ) : (
                  cart.map(item => (
                    <motion.div 
                      layout
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, scale: 0.9 }}
                      key={item.id}
                      className="p-3 bg-slate-50 border border-slate-100 rounded-xl flex items-center gap-3 group"
                    >
                      <div className="flex-1 overflow-hidden">
                        <p className="text-sm font-bold text-slate-900 truncate">{item.name}</p>
                        <p className="text-[10px] text-slate-500 font-mono">Qty Dec: -{item.quantity} | Total Stock: {products.find(p => p.id === item.id)?.stock}</p>
                      </div>
                      <div className="text-right flex flex-col items-end gap-1">
                        <button 
                          onClick={() => removeFromCart(item.id)}
                          className="text-slate-200 hover:text-red-500 transition-colors"
                        >
                          <Trash2 size={12} />
                        </button>
                      </div>
                    </motion.div>
                  ))
                )}
              </AnimatePresence>
            </div>

          <div className="flex-1 overflow-hidden flex flex-col items-center justify-center p-8 text-center text-slate-400 opacity-50">
            <QrCode size={48} strokeWidth={1} className="mb-4" />
            <p className="text-xs font-bold uppercase tracking-widest mb-1 font-mono">System Ready</p>
            <p className="text-[10px] font-mono whitespace-nowrap overflow-hidden">
              WAITING_FOR_CAMERA_LINK...
            </p>
          </div>
          </div>

          <div className="p-6 bg-slate-900 text-white shrink-0">
            <div className="flex justify-between items-end mb-6">
              <div>
                <p className="text-[10px] text-slate-400 uppercase font-black tracking-widest mb-1">Session Summary</p>
                <div className="text-3xl font-black tracking-tighter">
                  {cart.length} <span className="text-xs font-normal text-slate-500">ITEMS LOGGED</span>
                </div>
              </div>
              <button 
                onClick={() => setCart([])}
                className="text-[10px] text-slate-500 hover:text-red-400 font-bold uppercase tracking-widest transition-colors mb-2"
              >
                Reset Log
              </button>
            </div>
            
            <button 
              onClick={() => {
                alert(`Processed ${cart.length} item exits from repository.`);
                setCart([]);
              }}
              disabled={cart.length === 0}
              className="w-full bg-blue-600 hover:bg-blue-500 disabled:opacity-20 disabled:grayscale py-4 rounded-xl font-black uppercase tracking-widest text-xs transition-all active:scale-95 shadow-xl shadow-blue-900/20"
            >
              Commit Session
            </button>
          </div>
        </aside>
      </main>

      {/* System Status Footer */}
      <footer className="h-8 bg-slate-800 text-[9px] text-slate-500 px-8 flex items-center justify-between font-mono shrink-0 uppercase tracking-wider">
        <div className="flex items-center space-x-6">
          <span className="text-blue-500 font-bold">SYSTEM ACTIVE</span>
          <span>SENSORS: {isScanning ? 'ON' : 'OFF'}</span>
        </div>
      </footer>

      {/* Manual Entry Modal */}
      <AnimatePresence>
        {isManualEntryOpen && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-md"
          >
            <motion.div 
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-white w-full max-w-lg rounded-2xl overflow-hidden shadow-2xl border border-slate-200"
            >
              <div className="p-6 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-slate-900 text-white rounded-lg flex items-center justify-center">
                    <Package size={20} />
                  </div>
                  <div>
                    <h3 className="font-bold text-slate-900 text-lg">Inventory Registration</h3>
                    <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">Manual Entry Mode</p>
                  </div>
                </div>
                <button 
                  onClick={() => setIsManualEntryOpen(false)}
                  className="p-2 hover:bg-slate-100 rounded-full transition-colors text-slate-400"
                >
                  <X size={20} />
                </button>
              </div>

              <form onSubmit={handleAddProduct} className="p-6 space-y-4">
                {formError && (
                  <motion.div 
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: "auto", opacity: 1 }}
                    className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-3 text-red-600 text-xs font-bold uppercase tracking-wider"
                  >
                    <AlertCircle size={16} />
                    {formError}
                  </motion.div>
                )}

                <div className="grid grid-cols-2 gap-4">
                  <div className="col-span-2">
                    <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1 block">Product Name</label>
                    <input 
                      required
                      type="text" 
                      placeholder="e.g. Beyti Milk 1L"
                      className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:bg-white focus:border-blue-500 outline-none transition-all"
                      value={newProduct.name}
                      onChange={(e) => setNewProduct({...newProduct, name: e.target.value})}
                    />
                  </div>
                  <div>
                    <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1 block">Barcode Signature</label>
                    <input 
                      required
                      type="text" 
                      placeholder="622..."
                      className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:bg-white focus:border-blue-500 outline-none transition-all font-mono"
                      value={newProduct.barcode}
                      onChange={(e) => setNewProduct({...newProduct, barcode: e.target.value})}
                    />
                    <p className="text-[8px] text-slate-400 mt-1 uppercase font-bold tracking-tighter">Use EAN-13, UPC-A, or Alphanumeric ID (4-20 chars)</p>
                  </div>
                  <div>
                    <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1 block">Shelf Location</label>
                    <input 
                      required
                      type="text" 
                      placeholder="A1-R1"
                      className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:bg-white focus:border-blue-500 outline-none transition-all"
                      value={newProduct.location}
                      onChange={(e) => setNewProduct({...newProduct, location: e.target.value})}
                    />
                  </div>
                  <div>
                    <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1 block">Unit Price (EGP)</label>
                    <input 
                      required
                      type="number" 
                      step="0.01"
                      placeholder="0.00"
                      className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:bg-white focus:border-blue-500 outline-none transition-all"
                      value={newProduct.price}
                      onChange={(e) => setNewProduct({...newProduct, price: e.target.value})}
                    />
                  </div>
                  <div>
                    <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1 block">Expiry Date</label>
                    <input 
                      required
                      type="date" 
                      min={new Date().toISOString().split('T')[0]}
                      className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:bg-white focus:border-blue-500 outline-none transition-all"
                      value={newProduct.expiry}
                      onChange={(e) => setNewProduct({...newProduct, expiry: e.target.value})}
                    />
                  </div>
                  <div>
                    <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1 block">Category</label>
                    <select
                      className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:bg-white focus:border-blue-500 outline-none transition-all"
                      value={newProduct.category}
                      onChange={(e) => setNewProduct({...newProduct, category: e.target.value})}
                    >
                      <option value="Dairy & Eggs">Dairy & Eggs</option>
                      <option value="Beverages">Beverages</option>
                      <option value="Snacks">Snacks</option>
                      <option value="Canned & Packaged">Canned & Packaged</option>
                      <option value="General">General</option>
                    </select>
                  </div>
                  <div>
                    <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1 block">Initial Stock</label>
                    <input 
                      required
                      type="number" 
                      placeholder="0"
                      className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:bg-white focus:border-blue-500 outline-none transition-all"
                      value={newProduct.stock}
                      onChange={(e) => setNewProduct({...newProduct, stock: e.target.value})}
                    />
                  </div>
                </div>

                <div className="pt-4 flex gap-3">
                  <button 
                    type="button"
                    onClick={() => setIsManualEntryOpen(false)}
                    className="flex-1 px-4 py-2.5 border border-slate-200 text-slate-500 font-bold text-xs uppercase tracking-widest rounded-xl hover:bg-slate-50 transition-colors"
                  >
                    Cancel
                  </button>
                  <button 
                    type="submit"
                    className="flex-1 px-4 py-2.5 bg-blue-600 text-white font-bold text-xs uppercase tracking-widest rounded-xl hover:bg-blue-700 transition-colors shadow-lg shadow-blue-500/20"
                  >
                    Commit Entry
                  </button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <style dangerouslySetInnerHTML={{ __html: `
        @keyframes scan-fast {
          0% { transform: translateY(0); opacity: 0.2; }
          50% { opacity: 1; }
          100% { transform: translateY(355px); opacity: 0.2; }
        }
        .animate-scan-fast {
          animation: scan-fast 1.5s cubic-bezier(0.4, 0, 0.2, 1) infinite;
        }
        .custom-scrollbar::-webkit-scrollbar {
          width: 4px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
          background: transparent;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
          background: #e2e8f0;
          border-radius: 10px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover {
          background: #cbd5e1;
        }
      `}} />
    </div>

  );
}

